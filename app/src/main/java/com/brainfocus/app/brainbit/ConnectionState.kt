package com.brainfocus.app.brainbit

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class DevicesFound(val devices: List<BrainBitDevice>) : ScanState()
    object NoDevicesFound : ScanState()
    data class Error(val message: String) : ScanState()
}

data class BrainBitDevice(
    val name: String,
    val address: String
)
