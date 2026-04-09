package com.brainfocus.app.brainbit

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BrainBitManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isConnected: StateFlow<Boolean> = _connectionState.map { state ->
        state is ConnectionState.Connected
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _concentration = MutableStateFlow(0.5f)
    val concentration: StateFlow<Float> = _concentration.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val concentrationProcessor = ConcentrationProcessor()
    private var concentrationUpdateJob: Job? = null

    private var isRealDeviceConnected = false
    private var isSimulationMode = false

    suspend fun startScan(): List<BrainBitDevice> {
        _scanState.value = ScanState.Scanning

        delay(3000)

        val foundDevices = emptyList<BrainBitDevice>()

        _scanState.value = if (foundDevices.isEmpty()) {
            ScanState.NoDevicesFound
        } else {
            ScanState.DevicesFound(foundDevices)
        }
        
        return foundDevices
    }

    fun stopScan() {
        _scanState.value = ScanState.Idle
    }

    suspend fun connect(device: BrainBitDevice) {
        if (isSimulationMode) {
            stopSimulation()
        }
        
        _connectionState.value = ConnectionState.Connecting
        isRealDeviceConnected = false

        delay(1500)
        
        isRealDeviceConnected = true
        _connectionState.value = ConnectionState.Connected

        concentrationUpdateJob = scope.launch {
            while (isActive && isRealDeviceConnected) {
                val conc = concentrationProcessor.processSamples()
                _concentration.value = conc
                delay(100)
            }
        }
    }

    fun startSimulation() {
        if (isRealDeviceConnected) return
        
        isSimulationMode = true
        _connectionState.value = ConnectionState.Connected

        concentrationUpdateJob = scope.launch {
            while (isActive && isSimulationMode) {
                val conc = concentrationProcessor.processSamples()
                _concentration.value = conc
                delay(100)
            }
        }
    }

    fun stopSimulation() {
        isSimulationMode = false
        concentrationUpdateJob?.cancel()
        if (!isRealDeviceConnected) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun disconnect() {
        isRealDeviceConnected = false
        isSimulationMode = false
        concentrationUpdateJob?.cancel()
        concentrationProcessor.reset()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun isInSimulationMode(): Boolean = isSimulationMode

    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
