package com.brainfocus.app.ui.connection

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brainfocus.app.brainbit.BrainBitDevice
import com.brainfocus.app.brainbit.BrainBitManager
import com.brainfocus.app.brainbit.ConnectionState
import com.brainfocus.app.brainbit.DeviceInfo
import com.brainfocus.app.brainbit.ScanState
import com.brainfocus.app.ui.game.GameViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ConnectionViewModel : ViewModel() {
    private var brainBitManager: BrainBitManager? = null
    private var connectJob: Job? = null
    private var connectionStateJob: Job? = null
    private var concentrationJob: Job? = null
    private var batteryJob: Job? = null
    private var deviceInfoJob: Job? = null
    private var scanJob: Job? = null
    private var gameViewModel: GameViewModel? = null

    private val _connectionState: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scanState: MutableStateFlow<ScanState> = MutableStateFlow(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _concentration: MutableStateFlow<Float> = MutableStateFlow(0.5f)
    val concentration: StateFlow<Float> = _concentration.asStateFlow()

    private val _batteryLevel: MutableStateFlow<Int?> = MutableStateFlow(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _deviceInfo: MutableStateFlow<DeviceInfo?> = MutableStateFlow(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    fun setGameViewModel(viewModel: GameViewModel) {
        gameViewModel = viewModel
    }

    fun initialize(context: Context) {
        if (brainBitManager == null) {
            brainBitManager = BrainBitManager(context.applicationContext)
        }
    }

    fun startScan(context: Context) {
        initialize(context)
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val manager = brainBitManager!!
            manager.scanState.collect { state ->
                _scanState.value = state
            }
        }
        viewModelScope.launch {
            brainBitManager?.startScan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        brainBitManager?.stopScan()
    }

    fun connect(context: Context, device: BrainBitDevice) {
        initialize(context)
        connectionStateJob?.cancel()
        concentrationJob?.cancel()
        batteryJob?.cancel()
        deviceInfoJob?.cancel()

        val manager = brainBitManager!!
        connectionStateJob = viewModelScope.launch {
            manager.connectionState.collect { state ->
                _connectionState.value = state
            }
        }
        concentrationJob = viewModelScope.launch {
            manager.concentration.collect { value ->
                _concentration.value = value
            }
        }
        batteryJob = viewModelScope.launch {
            manager.batteryLevel.collect { level ->
                _batteryLevel.value = level
            }
        }
        deviceInfoJob = viewModelScope.launch {
            manager.deviceInfo.collect { info ->
                _deviceInfo.value = info
            }
        }
        viewModelScope.launch {
            manager.connect(device)
        }
    }

    fun disconnect() {
        // Cancel all connection-related jobs when disconnecting
        connectionStateJob?.cancel()
        connectionStateJob = null
        concentrationJob?.cancel()
        concentrationJob = null
        batteryJob?.cancel()
        batteryJob = null
        deviceInfoJob?.cancel()
        deviceInfoJob = null

        brainBitManager?.let { manager ->
            manager.disconnect()
        }

        // Reset connection state immediately so UI reflects disconnected state
        _connectionState.value = ConnectionState.Disconnected
        _concentration.value = 0.5f
        _batteryLevel.value = null
        _deviceInfo.value = null

        // Reset game state to allow fresh start after reconnect
        gameViewModel?.reset()
    }

    fun isConnected(): Boolean = brainBitManager?.isConnected?.value == true

    fun getManager(): BrainBitManager? = brainBitManager

    override fun onCleared() {
        super.onCleared()
        connectionStateJob?.cancel()
        concentrationJob?.cancel()
        batteryJob?.cancel()
        deviceInfoJob?.cancel()
        scanJob?.cancel()
        connectJob?.cancel()
        brainBitManager?.destroy()
    }
}

class ConnectionViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConnectionViewModel() as T
    }
}
