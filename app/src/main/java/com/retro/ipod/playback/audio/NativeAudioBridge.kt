package com.retro.ipod.playback.audio

import android.util.Log

/**
 * JNI bridge connecting Kotlin to C++ / Google Oboe for Low-Latency,
 * Exclusive Bit-Perfect audio streaming directly to USB DAC endpoints.
 */
object NativeAudioBridge {

    private const val TAG = "NativeAudioBridge"
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("retro_ipod_audio")
            isNativeLoaded = true
            Log.i(TAG, "Loaded retro_ipod_audio native library successfully. Oboe version: ${getOboeVersion()}")
        } catch (e: Throwable) {
            isNativeLoaded = false
            Log.d(TAG, "Native library retro_ipod_audio not loaded (using high-resolution AudioTrack HAL fallback): ${e.message}")
        }
    }

    fun isLoaded(): Boolean = isNativeLoaded

    external fun getOboeVersion(): String

    external fun initAudioStream(sampleRate: Int, channelCount: Int, bitDepth: Int): Boolean

    external fun writePcmBuffer(buffer: ByteArray, sizeInBytes: Int): Int

    external fun closeAudioStream()

    external fun getHardwareSampleRate(): Int
}
