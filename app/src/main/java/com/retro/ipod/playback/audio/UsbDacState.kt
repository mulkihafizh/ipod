package com.retro.ipod.playback.audio

/**
 * Hardware representation of a connected USB DAC audio endpoint.
 */
data class UsbDacState(
    val isConnected: Boolean = false,
    val isClaimed: Boolean = false,
    val deviceName: String = "",
    val manufacturerName: String = "",
    val vendorId: Int = 0,
    val productId: Int = 0,
    val interfaceCount: Int = 0,
    val hasAudioStreamingInterface: Boolean = false,
    val isExclusiveModeEnabled: Boolean = false,
    val lockedSampleRateKhz: Double = 44.1,
    val lockedBitDepth: Int = 16,
    val routeName: String = "DIRECT" // "USB-DAC" or "DIRECT"
) {
    val statusBadgeText: String
        get() = when {
            deviceName.isNotBlank() && !deviceName.equals("DIRECT", ignoreCase = true) && !deviceName.equals("Internal DAC", ignoreCase = true) -> deviceName
            isConnected && deviceName.isNotBlank() && !deviceName.equals("DIRECT", ignoreCase = true) -> deviceName
            isConnected -> "USB-DAC"
            else -> "DIRECT"
        }
}
