package com.brainfocus.app.brainbit

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.neurosdk2.neuro.BrainBit
import com.neurosdk2.neuro.Scanner
import com.neurosdk2.neuro.Sensor
import com.neurosdk2.neuro.interfaces.BrainBitResistDataReceived
import com.neurosdk2.neuro.interfaces.BrainBitSignalDataReceived
import com.neurosdk2.neuro.types.SensorCommand
import com.neurosdk2.neuro.types.SensorFamily
import com.neurosdk2.neuro.types.SensorState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class EEGSample(
    val o1: Float,
    val o2: Float,
    val t3: Float,
    val t4: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class ResistanceSample(
    val o1: Float,
    val o2: Float,
    val t3: Float,
    val t4: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceInfo(
    val name: String,
    val address: String,
    val serialNumber: String
)

class BrainBitManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val TAG = "BrainBitManager"

    private var scanner: Scanner? = null
    private var brainBit: BrainBit? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isConnected: StateFlow<Boolean> = _connectionState.map { state ->
        state is ConnectionState.Connected
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _concentration = MutableStateFlow(0.5f)
    val concentration: StateFlow<Float> = _concentration.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _rawEEGData = MutableSharedFlow<EEGSample>(extraBufferCapacity = 64)
    val rawEEGData: SharedFlow<EEGSample> = _rawEEGData

    private val _resistanceData = MutableStateFlow<ResistanceSample?>(null)
    val resistanceData: StateFlow<ResistanceSample?> = _resistanceData.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val concentrationProcessor = ConcentrationProcessor()
    private var connectionTimeoutJob: Job? = null
    private val batteryBuffer = mutableListOf<Int>()
    private val batteryBufferSize = 5

    private fun clearBleCache(): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter ?: return false
            val method = adapter.javaClass.getMethod("clearGattCache")
            method.invoke(adapter) as Boolean
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось очистить BLE-кэш: ${e.message}")
            false
        }
    }

    private fun smoothBatteryValue(newValue: Int): Int {
        batteryBuffer.add(newValue)
        if (batteryBuffer.size > batteryBufferSize) {
            batteryBuffer.removeAt(0)
        }
        val sorted = batteryBuffer.sorted()
        return sorted[sorted.size / 2]
    }

    suspend fun startScan(): List<BrainBitDevice> {
        val deferred = CompletableDeferred<List<BrainBitDevice>>()

        try {
            _scanState.value = ScanState.Scanning
            Log.d(TAG, "Начало сканирования...")

            clearBleCache()
            delay(500)

            scanner?.close()
            scanner = null

            scanner = Scanner(SensorFamily.SensorLEBrainBit)
            Log.d(TAG, "Scanner создан")

            scanner?.sensorsChanged = object : Scanner.ScannerCallback {
                override fun onSensorListChanged(scanner: Scanner, sensors: List<com.neurosdk2.neuro.types.SensorInfo>) {
                    Log.d(TAG, "onSensorListChanged: найдено ${sensors.size} устройств")
                    sensors.forEach { info ->
                        Log.d(TAG, "  Устройство: ${info.name}, MAC: ${info.address}, RSSI: ${info.rssi}")
                    }

                    val devices = sensors
                        .filter { it.rssi >= -85 }
                        .map { info ->
                            BrainBitDevice(
                                name = info.name,
                                address = info.address,
                                serialNumber = info.serialNumber ?: ""
                            )
                        }

                    Log.d(TAG, "После фильтрации RSSI: ${devices.size} устройств")
                    if (devices.isNotEmpty()) {
                        _scanState.value = ScanState.DevicesFound(devices)
                    }
                }
            }

            scanner?.start()
            Log.d(TAG, "Scanner запущен")

            scope.launch {
                delay(10000)
                scanner?.stop()
                Log.d(TAG, "Scanner остановлен")

                val allSensors = scanner?.sensors ?: emptyList()
                Log.d(TAG, "Всего сенсоров после сканирования: ${allSensors.size}")
                allSensors.forEach { info ->
                    Log.d(TAG, "  Сенсор: ${info.name}, MAC: ${info.address}, RSSI: ${info.rssi}")
                }

                val devices = allSensors
                    .filter { it.rssi >= -85 }
                    .map { info ->
                        BrainBitDevice(
                            name = info.name,
                            address = info.address,
                            serialNumber = info.serialNumber ?: ""
                        )
                    }

                if (devices.isEmpty()) {
                    _scanState.value = ScanState.NoDevicesFound
                    Log.d(TAG, "Нет устройств")
                } else {
                    _scanState.value = ScanState.DevicesFound(devices)
                    Log.d(TAG, "Найдено ${devices.size} устройств")
                }

                deferred.complete(devices)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений Bluetooth: ${e.message}")
            _scanState.value = ScanState.Error("Нет разрешений Bluetooth")
            deferred.complete(emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сканирования: ${e.message}")
            _scanState.value = ScanState.Error(e.message ?: "Ошибка сканирования")
            deferred.complete(emptyList())
        }

        return deferred.await()
    }

    fun stopScan() {
        scanner?.stop()
        _scanState.value = ScanState.Idle
    }

    suspend fun connect(device: BrainBitDevice) {
        _connectionState.value = ConnectionState.Connecting
        Log.d(TAG, "Подключение к ${device.name} (${device.address})")

        try {
            val deviceInfo = scanner?.sensors?.find { it.address == device.address }
                ?: throw Exception("Устройство не найдено в списке сенсоров")

            Log.d(TAG, "Создание сенсора (автоматическое подключение)...")
            val sensor = scanner?.createSensor(deviceInfo)
            brainBit = sensor as? BrainBit ?: throw Exception("Устройство не является BrainBit")

            _deviceInfo.value = DeviceInfo(
                name = device.name,
                address = device.address,
                serialNumber = device.serialNumber
            )

            val currentState = brainBit?.state
            Log.d(TAG, "Состояние после createSensor: $currentState")

            if (currentState == SensorState.StateInRange) {
                Log.d(TAG, "Устройство уже подключено после createSensor")
                setupAfterConnection()
                return
            }

            if (currentState == SensorState.StateOutOfRange) {
                Log.d(TAG, "Устройство не подключено, вызываем connect()...")

                val connectionResult = CompletableDeferred<Boolean>()

                brainBit?.sensorStateChanged = object : Sensor.SensorStateChanged {
                    override fun onStateChanged(state: SensorState) {
                        Log.d(TAG, "sensorStateChanged: $state")
                        when (state) {
                            SensorState.StateInRange -> {
                                connectionResult.complete(true)
                            }
                            SensorState.StateOutOfRange -> {
                                connectionResult.complete(false)
                            }
                            else -> {}
                        }
                    }
                }

                connectionTimeoutJob = scope.launch {
                    delay(10000)
                    if (!connectionResult.isCompleted) {
                        connectionResult.complete(false)
                    }
                }

                brainBit?.connect()

                val success = connectionResult.await()
                connectionTimeoutJob?.cancel()

                if (!success) {
                    Log.e(TAG, "Подключение не удалось")
                    brainBit?.disconnect()
                    brainBit?.close()
                    brainBit = null
                    _connectionState.value = ConnectionState.Error("Таймаут подключения")
                    return
                }

                setupAfterConnection()
            }

            brainBit?.sensorStateChanged = object : Sensor.SensorStateChanged {
                override fun onStateChanged(state: SensorState) {
                    Log.d(TAG, "sensorStateChanged: $state")
                    when (state) {
                        SensorState.StateInRange -> {
                            _connectionState.value = ConnectionState.Connected
                        }
                        SensorState.StateOutOfRange -> {
                            _connectionState.value = ConnectionState.Disconnected
                            Log.d(TAG, "Устройство вне зоны действия")
                        }
                        else -> {}
                    }
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Нет разрешений Bluetooth: ${e.message}")
            _connectionState.value = ConnectionState.Error("Нет разрешений Bluetooth")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка подключения: ${e.message}")
            _connectionState.value = ConnectionState.Error(e.message ?: "Ошибка подключения")
        }
    }

    private fun setupAfterConnection() {
        Log.d(TAG, "Настройка после подключения")

        batteryBuffer.clear()

        brainBit?.batteryChanged = object : Sensor.BatteryChanged {
            override fun onBatteryChanged(power: Int) {
                Log.d(TAG, "batteryChanged: $power%")
                val smoothed = smoothBatteryValue(power)
                _batteryLevel.value = smoothed
            }
        }

        val battery = brainBit?.battPower
        if (battery != null && battery > 0) {
            val smoothed = smoothBatteryValue(battery)
            _batteryLevel.value = smoothed
        }
        Log.d(TAG, "Батарея: ${_batteryLevel.value}%")

        _connectionState.value = ConnectionState.Connected
        Log.d(TAG, "Подключено успешно")

        startReceivingEEG()
        startResistanceTest()
    }

    private fun startReceivingEEG() {
        try {
            Log.d(TAG, "Запуск приёма EEG сигнала...")
            brainBit?.brainBitSignalDataReceived = object : BrainBitSignalDataReceived {
                override fun onBrainBitSignalDataReceived(data: Array<out com.neurosdk2.neuro.types.BrainBitSignalData>) {
                    for (sample in data) {
                        val eegSample = EEGSample(
                            o1 = sample.getO1().toFloat(),
                            o2 = sample.getO2().toFloat(),
                            t3 = sample.getT3().toFloat(),
                            t4 = sample.getT4().toFloat()
                        )
                        _rawEEGData.tryEmit(eegSample)

                        val allSamples = listOf(eegSample.o1, eegSample.o2, eegSample.t3, eegSample.t4).toFloatArray()
                        val concentration = concentrationProcessor.processSamples(allSamples)
                        _concentration.value = concentration
                    }
                }
            }
            brainBit?.execCommand(SensorCommand.StartSignal)
            Log.d(TAG, "EEG сигнал запущен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска сигнала: ${e.message}")
            _connectionState.value = ConnectionState.Error("Ошибка запуска сигнала: ${e.message}")
        }
    }

    fun startResistanceTest() {
        try {
            Log.d(TAG, "Запуск теста сопротивления...")
            brainBit?.brainBitResistDataReceived = object : BrainBitResistDataReceived {
                override fun onBrainBitResistDataReceived(data: com.neurosdk2.neuro.types.BrainBitResistData) {
                    val resistSample = ResistanceSample(
                        o1 = data.getO1().toFloat(),
                        o2 = data.getO2().toFloat(),
                        t3 = data.getT3().toFloat(),
                        t4 = data.getT4().toFloat()
                    )
                    _resistanceData.value = resistSample
                }
            }
            brainBit?.execCommand(SensorCommand.StartResist)
            Log.d(TAG, "Тест сопротивления запущен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска теста сопротивления: ${e.message}")
        }
    }

    fun stopResistanceTest() {
        try {
            brainBit?.execCommand(SensorCommand.StopResist)
            Log.d(TAG, "Тест сопротивления остановлен")
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка остановки теста сопротивления: ${e.message}")
        }
    }

    fun disconnect() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = null

        try {
            Log.d(TAG, "Отключение...")
            brainBit?.execCommand(SensorCommand.StopSignal)
            stopResistanceTest()
            brainBit?.disconnect()
            brainBit?.close()
            Log.d(TAG, "Отключено")
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка при отключении: ${e.message}")
        }

        scanner?.close()
        brainBit = null
        scanner = null
        batteryBuffer.clear()
        _batteryLevel.value = null
        _deviceInfo.value = null
        _resistanceData.value = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
