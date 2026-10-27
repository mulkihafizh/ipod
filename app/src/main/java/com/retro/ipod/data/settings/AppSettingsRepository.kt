package com.retro.ipod.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ipod_settings")

/**
 * Repository for managing and persisting user configuration via Jetpack Preferences DataStore.
 */
class AppSettingsRepository(
    private val context: Context
) {

    private object PreferencesKeys {
        val USB_EXCLUSIVE_MODE = booleanPreferencesKey("usb_exclusive_mode")
        val BIT_PERFECT_OUTPUT = booleanPreferencesKey("bit_perfect_output")
        val LYRICS_MODE = stringPreferencesKey("lyrics_mode")
        val BG_MODE = stringPreferencesKey("bg_mode")
        val GLOW_ENABLED = booleanPreferencesKey("glow_enabled")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val LINE_GAP_DP = intPreferencesKey("line_gap_dp")
        val SKIN_THEME = stringPreferencesKey("skin_theme")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val MUSIC_FOLDER_PATH = stringPreferencesKey("music_folder_path")
        val MUSIC_FOLDER_URI = stringPreferencesKey("music_folder_uri")
    }

    /**
     * Flow of [AppSettings] emitted whenever any setting is modified.
     */
    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val usbExclusiveMode = preferences[PreferencesKeys.USB_EXCLUSIVE_MODE] ?: false
            val bitPerfectOutput = preferences[PreferencesKeys.BIT_PERFECT_OUTPUT] ?: true

            val lyricsModeString = preferences[PreferencesKeys.LYRICS_MODE] ?: LyricsDisplayMode.MODERN_FLOW.name
            val lyricsMode = try {
                LyricsDisplayMode.valueOf(lyricsModeString)
            } catch (e: IllegalArgumentException) {
                LyricsDisplayMode.MODERN_FLOW
            }

            val bgModeString = preferences[PreferencesKeys.BG_MODE] ?: BgMode.BLURRED_COVER.name
            val bgMode = try {
                BgMode.valueOf(bgModeString)
            } catch (e: IllegalArgumentException) {
                BgMode.BLURRED_COVER
            }

            val glowEnabled = preferences[PreferencesKeys.GLOW_ENABLED] ?: true
            val fontScale = preferences[PreferencesKeys.FONT_SCALE] ?: 1.0f
            val lineGapDp = preferences[PreferencesKeys.LINE_GAP_DP] ?: 16

            val skinThemeString = preferences[PreferencesKeys.SKIN_THEME] ?: SkinTheme.CLASSIC_WHITE.name
            val skinTheme = try {
                SkinTheme.valueOf(skinThemeString)
            } catch (e: IllegalArgumentException) {
                SkinTheme.CLASSIC_WHITE
            }

            val hapticFeedback = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true
            val musicFolderPath = preferences[PreferencesKeys.MUSIC_FOLDER_PATH]
            val musicFolderUri = preferences[PreferencesKeys.MUSIC_FOLDER_URI]

            AppSettings(
                usbExclusiveMode = usbExclusiveMode,
                bitPerfectOutput = bitPerfectOutput,
                lyricsMode = lyricsMode,
                bgMode = bgMode,
                glowEnabled = glowEnabled,
                fontScale = fontScale,
                lineGapDp = lineGapDp,
                skinTheme = skinTheme,
                hapticFeedbackEnabled = hapticFeedback,
                musicFolderPath = musicFolderPath,
                musicFolderUri = musicFolderUri
            )
        }

    /**
     * Toggles exclusive USB DAC claim mode (bypasses Android AudioFlinger mixer).
     */
    suspend fun setUsbExclusiveMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USB_EXCLUSIVE_MODE] = enabled
        }
    }

    /**
     * Toggles Bit-Perfect audio output format alignment.
     */
    suspend fun setBitPerfectOutput(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIT_PERFECT_OUTPUT] = enabled
        }
    }

    /**
     * Switches lyrics view mode (Modern Fluid Syllables vs OG Monochrome).
     */
    suspend fun setLyricsMode(mode: LyricsDisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LYRICS_MODE] = mode.name
        }
    }

    /**
     * Switches lyrics and background backdrop effect.
     */
    suspend fun setBgMode(mode: BgMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BG_MODE] = mode.name
        }
    }

    /**
     * Toggles word-level bloom glow animation.
     */
    suspend fun setGlowEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GLOW_ENABLED] = enabled
        }
    }

    /**
     * Adjusts lyrics font scale factor (0.8f .. 1.4f).
     */
    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SCALE] = scale
        }
    }

    /**
     * Adjusts vertical line spacing between lyric sentences (10dp .. 32dp).
     */
    suspend fun setLineGapDp(gapDp: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LINE_GAP_DP] = gapDp
        }
    }

    /**
     * Updates the iPod casing skin theme.
     */
    suspend fun setSkinTheme(theme: SkinTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIN_THEME] = theme.name
        }
    }

    /**
     * Toggles Click Wheel tactile haptic vibration.
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = enabled
        }
    }

    /**
     * Sets the dedicated music folder path and URI to restrict library indexing.
     */
    suspend fun setMusicFolder(path: String?, uriString: String?) {
        context.dataStore.edit { preferences ->
            if (path != null) {
                preferences[PreferencesKeys.MUSIC_FOLDER_PATH] = path
            } else {
                preferences.remove(PreferencesKeys.MUSIC_FOLDER_PATH)
            }
            if (uriString != null) {
                preferences[PreferencesKeys.MUSIC_FOLDER_URI] = uriString
            } else {
                preferences.remove(PreferencesKeys.MUSIC_FOLDER_URI)
            }
        }
    }

    /**
     * Resets all settings to factory iPod defaults.
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
