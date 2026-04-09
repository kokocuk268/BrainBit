package com.brainfocus.app.ui.connection

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brainfocus.app.brainbit.BrainBitDevice
import com.brainfocus.app.brainbit.BrainBitManager
import com.brainfocus.app.brainbit.ConnectionState
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

    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    private val _canStartGame = MutableStateFlow(false)
    val canStartGame: StateFlow<Boolean> = _canStartGame.asStateFlow()

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
                    updateCanStartGame()
                }
            }
            launch {
                manager.concentration.collect { value ->
                    _concentration.value = value
                }
            }
            manager.connect(device)
        }
    }

    fun startSimulation() {
        brainBitManager?.let { manager ->
            manager.startSimulation()
            viewModelScope.launch {
                launch {
                    manager.connectionState.collect { state ->
                        _connectionState.value = state
                        updateCanStartGame()
                    }
                }
                launch {
                    manager.concentration.collect { value ->
                        _concentration.value = value
                    }
                }
            }
        }
    }

    fun stopSimulation() {
        brainBitManager?.stopSimulation()
        updateCanStartGame()
    }

    fun setTestMode(enabled: Boolean) {
        _isTestMode.value = enabled
        updateCanStartGame()
        
        if (enabled && brainBitManager != null) {
            if (!brainBitManager!!.isConnected.value) {
                startSimulation()
            }
        } else if (!enabled && brainBitManager?.isInSimulationMode() == true) {
            disconnect()
        }
    }

    fun disconnect() {
        brainBitManager?.disconnect()
        updateCanStartGame()
    }

    fun isConnectedToDevice(): Boolean {
        return brainBitManager?.isConnected?.value == true
    }

    fun isInSimulationMode(): Boolean {
        return brainBitManager?.isInSimulationMode() == true
    }

    private fun updateCanStartGame() {
        val isConnected = brainBitManager?.isConnected?.value == true
        val isTest = _isTestMode.value
        _canStartGame.value = isConnected || isTest
    }

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
