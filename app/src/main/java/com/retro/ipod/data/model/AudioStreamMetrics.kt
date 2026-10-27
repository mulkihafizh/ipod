package com.retro.ipod.data.model

/**
 * Encapsulates the exact technical parameters of an audio stream.
 *
 * @param codec Normalized codec tag (e.g., "FLAC", "MP3", "WAV", "ALAC", "AAC", "OGG")
 * @param sampleRateKhz Sample rate expressed in kHz (e.g. 44.1, 48.0, 96.0, 192.0)
 * @param bitDepth PCM bit depth (e.g. 16, 24, 32)
 */
data class AudioStreamMetrics(
    val codec: String,
    val sampleRateKhz: Double,
    val bitDepth: Int
) {
    /**
     * Formats stream rate for the iPod Mini Status Bar (e.g. "FLAC 44.1kHz / 32bit" or "FLAC 96.0kHz / 24bit")
     */
    fun formattedBadge(): String {
        return "$codec ${sampleRateKhz}kHz / ${bitDepth}bit"
    }

    companion object {
        val DEFAULT_STANDARD = AudioStreamMetrics(
            codec = "MP3",
            sampleRateKhz = 44.1,
            bitDepth = 16
        )
    }
}
