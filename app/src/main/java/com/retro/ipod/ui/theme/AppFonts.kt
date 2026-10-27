package com.retro.ipod.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

/**
 * Typography management providing:
 * - SF Pro fonts for the Modern Lyrics panel (loaded from assets/fonts/sf-pro)
 * - Helvetica / Helvetica Neue as Main UI Font & Classic Lyrics (loaded from assets/fonts/helvetica)
 */
object AppFonts {

    private val typefaceCache = mutableMapOf<String, FontFamily>()

    @Composable
    fun sfProDisplay(weight: FontWeight = FontWeight.Normal): FontFamily =
        sfProDisplay(LocalContext.current, weight)

    /**
     * Resolves SF Pro Display font with given weight from assets.
     */
    fun sfProDisplay(context: Context, weight: FontWeight = FontWeight.Normal): FontFamily {
        val fontFile = when (weight) {
            FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black -> "fonts/sf-pro/SF-Pro-Display-Bold.otf"
            FontWeight.SemiBold -> "fonts/sf-pro/SF-Pro-Display-Semibold.otf"
            FontWeight.Medium -> "fonts/sf-pro/SF-Pro-Display-Medium.otf"
            else -> "fonts/sf-pro/SF-Pro-Display-Regular.otf"
        }

        return typefaceCache.getOrPut(fontFile) {
            try {
                val tf = Typeface.createFromAsset(context.assets, fontFile)
                FontFamily(tf)
            } catch (e: Exception) {
                try {
                    val flatName = fontFile.substringAfterLast('/')
                    val tfFlat = Typeface.createFromAsset(context.assets, "fonts/$flatName")
                    FontFamily(tfFlat)
                } catch (_: Exception) {
                    FontFamily.SansSerif
                }
            }
        }
    }

    @Composable
    fun sfProRounded(weight: FontWeight = FontWeight.Normal): FontFamily =
        sfProRounded(LocalContext.current, weight)

    /**
     * Resolves SF Pro Rounded font with given weight from assets.
     */
    fun sfProRounded(context: Context, weight: FontWeight = FontWeight.Normal): FontFamily {
        val fontFile = when (weight) {
            FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black, FontWeight.SemiBold -> "fonts/sf-pro/SF-Pro-Rounded-Bold.otf"
            else -> "fonts/sf-pro/SF-Pro-Rounded-Regular.otf"
        }

        return typefaceCache.getOrPut(fontFile) {
            try {
                val tf = Typeface.createFromAsset(context.assets, fontFile)
                FontFamily(tf)
            } catch (e: Exception) {
                try {
                    val flatName = fontFile.substringAfterLast('/')
                    val tfFlat = Typeface.createFromAsset(context.assets, "fonts/$flatName")
                    FontFamily(tfFlat)
                } catch (_: Exception) {
                    FontFamily.SansSerif
                }
            }
        }
    }

    @Composable
    fun helvetica(weight: FontWeight = FontWeight.Normal): FontFamily =
        helvetica(LocalContext.current, weight)

    /**
     * Resolves Helvetica font from assets for the Main app interface and Classic lyrics.
     */
    fun helvetica(context: Context, weight: FontWeight = FontWeight.Normal): FontFamily {
        val isBold = weight == FontWeight.Bold || weight == FontWeight.ExtraBold || weight == FontWeight.SemiBold
        val fontFile = if (isBold) "fonts/helvetica/Helvetica-Bold.ttf" else "fonts/helvetica/Helvetica.ttf"

        return typefaceCache.getOrPut(fontFile) {
            try {
                val tf = Typeface.createFromAsset(context.assets, fontFile)
                FontFamily(tf)
            } catch (e: Exception) {
                try {
                    val flatName = if (isBold) "fonts/Helvetica-Bold.ttf" else "fonts/Helvetica.ttf"
                    val tfFlat = Typeface.createFromAsset(context.assets, flatName)
                    FontFamily(tfFlat)
                } catch (_: Exception) {
                    FontFamily.SansSerif
                }
            }
        }
    }

    /**
     * Material3 Typography instance based on Helvetica.
     */
    @Composable
    fun ipodTypography(): Typography {
        val regular = helvetica(FontWeight.Normal)
        val bold = helvetica(FontWeight.Bold)
        val semiBold = helvetica(FontWeight.SemiBold)
        val medium = helvetica(FontWeight.Medium)

        return Typography(
            displayLarge = TextStyle(fontFamily = bold),
            displayMedium = TextStyle(fontFamily = bold),
            displaySmall = TextStyle(fontFamily = bold),
            headlineLarge = TextStyle(fontFamily = bold),
            headlineMedium = TextStyle(fontFamily = semiBold),
            headlineSmall = TextStyle(fontFamily = semiBold),
            titleLarge = TextStyle(fontFamily = bold),
            titleMedium = TextStyle(fontFamily = semiBold),
            titleSmall = TextStyle(fontFamily = medium),
            bodyLarge = TextStyle(fontFamily = regular),
            bodyMedium = TextStyle(fontFamily = regular),
            bodySmall = TextStyle(fontFamily = regular),
            labelLarge = TextStyle(fontFamily = medium),
            labelMedium = TextStyle(fontFamily = medium),
            labelSmall = TextStyle(fontFamily = regular)
        )
    }
}
