package com.retro.ipod.data.settings

/**
 * Persisted application configuration and user preferences for audio output,
 * lyrics rendering, and retro aesthetics.
 */
data class AppSettings(
    val usbExclusiveMode: Boolean = false,
    val bitPerfectOutput: Boolean = true,
    val lyricsMode: LyricsDisplayMode = LyricsDisplayMode.MODERN_FLOW,
    val bgMode: BgMode = BgMode.BLURRED_COVER,
    val glowEnabled: Boolean = true,
    val fontScale: Float = 1.0f,
    val lineGapDp: Int = 16,
    val skinTheme: SkinTheme = SkinTheme.CLASSIC_WHITE,
    val hapticFeedbackEnabled: Boolean = true,
    val musicFolderPath: String? = null,
    val musicFolderUri: String? = null
)
