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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionViewModel : ViewModel() {
    private var brainBitManager: BrainBitManager? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _concentration = MutableStateFlow(0.5f)
    val concentration: StateFlow<Float> = _concentration.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    fun initialize(context: Context) {
        if (brainBitManager == null) {
            brainBitManager = BrainBitManager(context.applicationContext)
        }
    }

    fun startScan(context: Context) {
        initialize(context)
        viewModelScope.launch {
            val manager = brainBitManager!!
            launch {
                manager.scanState.collect { state ->
                    _scanState.value = state
                }
            }
            manager.startScan()
        }
    }

    fun stopScan() {
        brainBitManager?.stopScan()
    }

    fun connect(context: Context, device: BrainBitDevice) {
        initialize(context)
        viewModelScope.launch {
            val manager = brainBitManager!!
            launch {
                manager.connectionState.collect { state ->
                    _connectionState.value = state
                }
            }
            launch {
                manager.concentration.collect { value ->
                    _concentration.value = value
                }
            }
            launch {
                manager.batteryLevel.collect { level ->
                    _batteryLevel.value = level
                }
            }
            launch {
                manager.deviceInfo.collect { info ->
                    _deviceInfo.value = info
                }
            }
            manager.connect(device)
        }
    }

    fun disconnect() {
        brainBitManager?.disconnect()
    }

    fun isConnected(): Boolean = brainBitManager?.isConnected?.value == true

    fun getManager(): BrainBitManager? = brainBitManager

    override fun onCleared() {
        super.onCleared()
        brainBitManager?.destroy()
    }
}

class ConnectionViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ConnectionViewModel() as T
    }
}
