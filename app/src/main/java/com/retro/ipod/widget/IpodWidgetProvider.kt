package com.retro.ipod.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import com.retro.ipod.IpodApplication
import com.retro.ipod.MainActivity
import com.retro.ipod.R
import com.retro.ipod.data.art.CoverArtResolver
import com.retro.ipod.data.settings.AppSettings
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.playback.CurrentPlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android Home Screen Widget for Retro iPod.
 * Features full-width compact horizontal layout matching active skin theme:
 * - Left section: Album art cover (or retro grooved vinyl record), song title, artist/album, and audio format badge.
 * - Right section: Mini iPod Click Wheel matching chassis & wheel colors (preset or custom),
 *   with MENU, PREV, dedicated PLAY/PAUSE at bottom, and authentic smooth center SELECT button.
 */
class IpodWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val playerManager = try {
            IpodApplication.instance.playerManager
        } catch (_: Exception) {
            null
        }
        val state = playerManager?.playbackState?.value
        updateWidgetsInternal(context, appWidgetManager, appWidgetIds, state)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val playerManager = try {
            IpodApplication.instance.playerManager
        } catch (_: Exception) {
            null
        }

        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE -> {
                playerManager?.togglePlayPause()
            }
            ACTION_WIDGET_NEXT -> {
                playerManager?.skipToNext()
            }
            ACTION_WIDGET_PREV -> {
                playerManager?.skipToPrevious()
            }
            ACTION_WIDGET_OPEN -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(openIntent)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.retro.ipod.widget.ACTION_PLAY_PAUSE"
        const val ACTION_WIDGET_PREV = "com.retro.ipod.widget.ACTION_PREV"
        const val ACTION_WIDGET_NEXT = "com.retro.ipod.widget.ACTION_NEXT"
        const val ACTION_WIDGET_OPEN = "com.retro.ipod.widget.ACTION_OPEN"

        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        @Volatile
        private var currentArtPath: String? = null
        @Volatile
        private var currentArtRoundedBitmap: Bitmap? = null

        fun updateAllWidgets(
            context: Context,
            state: CurrentPlaybackState? = null,
            settings: AppSettings? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, IpodWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            val actualState = state ?: try {
                IpodApplication.instance.playerManager.playbackState.value
            } catch (_: Exception) {
                null
            }

            val actualSettings = settings ?: try {
                IpodApplication.instance.settingsRepository.currentSettings
            } catch (_: Exception) {
                AppSettings()
            }

            updateWidgetsInternal(context, appWidgetManager, appWidgetIds, actualState, actualSettings)
        }

        private fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            state: CurrentPlaybackState?,
            settings: AppSettings = try {
                IpodApplication.instance.settingsRepository.currentSettings
            } catch (_: Exception) {
                AppSettings()
            }
        ) {
            val currentSong = state?.currentSong
            val isPlaying = state?.isPlaying ?: false

            val palette = getWidgetPalette(settings)
            val views = RemoteViews(context.packageName, R.layout.widget_ipod_compact)

            // 1. Skeuomorphic Theme Background Bitmaps (rendered fresh to guarantee exact theme matching)
            val chassisBitmap = drawChassisBitmap(palette)
            val wheelBitmap = drawWheelBitmap(palette)
            val centerBitmap = drawCenterButtonBitmap(palette)

            views.setImageViewBitmap(R.id.widget_chassis_bg, chassisBitmap)
            views.setImageViewBitmap(R.id.widget_wheel_bg, wheelBitmap)
            views.setImageViewBitmap(R.id.widget_btn_center, centerBitmap)

            // 2. Click Wheel Buttons & Text Colors
            views.setTextColor(R.id.widget_btn_menu, palette.wheelTextColor)
            views.setInt(R.id.widget_btn_prev, "setColorFilter", palette.wheelTextColor)
            views.setInt(R.id.widget_btn_next, "setColorFilter", palette.wheelTextColor)
            views.setInt(R.id.widget_btn_play_pause_bottom, "setColorFilter", palette.wheelTextColor)

            // Single dedicated play/pause icon toggle on the bottom quadrant of the wheel
            val playPauseIconRes = if (isPlaying) {
                R.drawable.media3_notification_pause
            } else {
                R.drawable.media3_notification_play
            }
            views.setImageViewResource(R.id.widget_btn_play_pause_bottom, playPauseIconRes)

            // 3. Track Information & Details
            if (currentSong != null) {
                views.setTextViewText(R.id.widget_song_title, currentSong.title)
                views.setTextViewText(R.id.widget_artist_album, "${currentSong.artist} • ${currentSong.album}")
                views.setTextViewText(R.id.widget_format_badge, "${currentSong.codec} ${currentSong.sampleRateKhz}k")
            } else {
                views.setTextViewText(R.id.widget_song_title, "No Track Playing")
                views.setTextViewText(R.id.widget_artist_album, "Retro iPod Classic")
                views.setTextViewText(R.id.widget_format_badge, "BIT-PERFECT")
            }

            views.setTextColor(R.id.widget_song_title, palette.songTitleColor)
            views.setTextColor(R.id.widget_artist_album, palette.artistColor)
            views.setTextColor(R.id.widget_format_badge, if (palette.isLightChassis) 0xFF0277BD.toInt() else 0xFF4FC3F7.toInt())

            // 4. Stable, Flicker-Free Album Art Resolution with In-Memory Caching
            var needsAsyncArtLoad = false
            val songPath = currentSong?.filePath

            if (songPath == null) {
                currentArtPath = null
                currentArtRoundedBitmap = null
                views.setImageViewBitmap(
                    R.id.widget_album_art,
                    createDefaultRecordBitmap(sizePx = 160, title = null, artist = null)
                )
            } else if (currentArtPath == songPath && currentArtRoundedBitmap != null) {
                // Reuse existing cached rounded bitmap immediately without redrawing or reloading
                views.setImageViewBitmap(R.id.widget_album_art, currentArtRoundedBitmap)
            } else {
                // Check if CoverArtResolver already has the raw bitmap in memory
                val memoryBitmap = CoverArtResolver.getFromMemoryCache(songPath)
                if (memoryBitmap != null) {
                    val rounded = getRoundedCornerBitmap(memoryBitmap, cornerRadiusPx = 14f)
                    currentArtPath = songPath
                    currentArtRoundedBitmap = rounded
                    views.setImageViewBitmap(R.id.widget_album_art, rounded)
                } else {
                    // Render procedural record fallback once while async disk decode runs
                    val defaultVinylBitmap = createDefaultRecordBitmap(
                        sizePx = 160,
                        title = currentSong.title,
                        artist = currentSong.artist
                    )
                    views.setImageViewBitmap(R.id.widget_album_art, defaultVinylBitmap)
                    needsAsyncArtLoad = true
                }
            }

            // 5. Interactive PendingIntents
            // Tapping left section, MENU button, or center SELECT button opens the app
            val openPendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                Intent(context, IpodWidgetProvider::class.java).apply { action = ACTION_WIDGET_OPEN },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_left_section, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_menu, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_center, openPendingIntent)

            // Previous button
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
                Intent(context, IpodWidgetProvider::class.java).apply { action = ACTION_WIDGET_PREV },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            // Next button
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                1003,
                Intent(context, IpodWidgetProvider::class.java).apply { action = ACTION_WIDGET_NEXT },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Dedicated Play/Pause button on the bottom quadrant of the click wheel
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                1004,
                Intent(context, IpodWidgetProvider::class.java).apply { action = ACTION_WIDGET_PLAY_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause_bottom, playPausePendingIntent)

            // Commit atomic widget update
            appWidgetManager.updateAppWidget(appWidgetIds, views)

            // 6. Asynchronous Album Art Loading only if not already cached
            if (needsAsyncArtLoad && songPath != null) {
                widgetScope.launch {
                    val artBitmap: Bitmap? = CoverArtResolver.loadBitmap(songPath, maxDimension = 160)
                    if (artBitmap != null && currentSong.filePath == songPath) {
                        val roundedArt = getRoundedCornerBitmap(artBitmap, cornerRadiusPx = 14f)
                        currentArtPath = songPath
                        currentArtRoundedBitmap = roundedArt
                        views.setImageViewBitmap(R.id.widget_album_art, roundedArt)
                        appWidgetManager.updateAppWidget(appWidgetIds, views)
                    }
                }
            }
        }

        // =========================================================================
        // Theme Colors & Procedural Graphics
        // =========================================================================

        data class WidgetThemePalette(
            val chassisTop: Int,
            val chassisMid: Int,
            val chassisBottom: Int,
            val chamferBorder: Int,
            val wheelBase: Int,
            val wheelOuterShade: Int,
            val wheelBorder: Int,
            val centerBase: Int,
            val centerOuterShade: Int,
            val centerBorder: Int,
            val wheelTextColor: Int,
            val songTitleColor: Int,
            val artistColor: Int,
            val isLightChassis: Boolean
        )

        private fun getWidgetPalette(settings: AppSettings): WidgetThemePalette {
            return when (settings.skinTheme) {
                SkinTheme.U2_EDITION -> WidgetThemePalette(
                    chassisTop = 0xFF1E1E22.toInt(),
                    chassisMid = 0xFF121214.toInt(),
                    chassisBottom = 0xFF0C0C0E.toInt(),
                    chamferBorder = 0xFF2C2C34.toInt(),
                    wheelBase = 0xFFE53935.toInt(),
                    wheelOuterShade = 0xFFC62828.toInt(),
                    wheelBorder = 0xFFB71C1C.toInt(),
                    centerBase = 0xFF2C2C2E.toInt(),
                    centerOuterShade = 0xFF1C1C1E.toInt(),
                    centerBorder = 0xFF3A3A3E.toInt(),
                    wheelTextColor = 0xFFFFFFFF.toInt(),
                    songTitleColor = 0xFFFFFFFF.toInt(),
                    artistColor = 0xFFA2A2AC.toInt(),
                    isLightChassis = false
                )
                SkinTheme.BRUSHED_ALUMINUM -> WidgetThemePalette(
                    chassisTop = 0xFF34343A.toInt(),
                    chassisMid = 0xFF24242A.toInt(),
                    chassisBottom = 0xFF1C1C20.toInt(),
                    chamferBorder = 0xFF484852.toInt(),
                    wheelBase = 0xFF323236.toInt(),
                    wheelOuterShade = 0xFF252528.toInt(),
                    wheelBorder = 0xFF48484A.toInt(),
                    centerBase = 0xFF404044.toInt(),
                    centerOuterShade = 0xFF2C2C30.toInt(),
                    centerBorder = 0xFF525258.toInt(),
                    wheelTextColor = 0xFFE0E0E0.toInt(),
                    songTitleColor = 0xFFFFFFFF.toInt(),
                    artistColor = 0xFFA2A2AC.toInt(),
                    isLightChassis = false
                )
                SkinTheme.CUSTOM -> {
                    val skinRaw = settings.customSkinColor.toInt()
                    val rS = Color.red(skinRaw) / 255f
                    val gS = Color.green(skinRaw) / 255f
                    val bS = Color.blue(skinRaw) / 255f

                    val isLightChassis = (rS * 0.299f + gS * 0.587f + bS * 0.114f) > 0.5f

                    val topC = Color.rgb(
                        (rS * 1.08f).coerceIn(0f, 1f),
                        (gS * 1.08f).coerceIn(0f, 1f),
                        (bS * 1.08f).coerceIn(0f, 1f)
                    )
                    val midC = Color.rgb(
                        (rS * 0.92f).coerceIn(0f, 1f),
                        (gS * 0.92f).coerceIn(0f, 1f),
                        (bS * 0.92f).coerceIn(0f, 1f)
                    )
                    val botC = Color.rgb(
                        (rS * 0.76f).coerceIn(0f, 1f),
                        (gS * 0.76f).coerceIn(0f, 1f),
                        (bS * 0.76f).coerceIn(0f, 1f)
                    )
                    val chamferC = Color.rgb(
                        (rS * 0.65f).coerceIn(0f, 1f),
                        (gS * 0.65f).coerceIn(0f, 1f),
                        (bS * 0.65f).coerceIn(0f, 1f)
                    )

                    val wheelRaw = settings.customWheelColor.toInt()
                    val rW = Color.red(wheelRaw) / 255f
                    val gW = Color.green(wheelRaw) / 255f
                    val bW = Color.blue(wheelRaw) / 255f

                    val isLightWheel = (rW * 0.299f + gW * 0.587f + bW * 0.114f) > 0.5f

                    val wheelOuterShade = Color.rgb(
                        (rW * 0.92f).coerceIn(0f, 1f),
                        (gW * 0.92f).coerceIn(0f, 1f),
                        (bW * 0.92f).coerceIn(0f, 1f)
                    )
                    val centerMult = if (isLightWheel) 1.05f else 1.15f
                    val centerBase = Color.rgb(
                        (rW * centerMult).coerceIn(0f, 1f),
                        (gW * centerMult).coerceIn(0f, 1f),
                        (bW * centerMult).coerceIn(0f, 1f)
                    )
                    val wheelBorder = Color.rgb(
                        (rW * 0.80f).coerceIn(0f, 1f),
                        (gW * 0.80f).coerceIn(0f, 1f),
                        (bW * 0.80f).coerceIn(0f, 1f)
                    )

                    WidgetThemePalette(
                        chassisTop = topC,
                        chassisMid = midC,
                        chassisBottom = botC,
                        chamferBorder = chamferC,
                        wheelBase = wheelRaw,
                        wheelOuterShade = wheelOuterShade,
                        wheelBorder = wheelBorder,
                        centerBase = centerBase,
                        centerOuterShade = wheelRaw,
                        centerBorder = wheelBorder,
                        wheelTextColor = if (isLightWheel) 0xFF555555.toInt() else 0xFFEEEEEE.toInt(),
                        songTitleColor = if (isLightChassis) 0xFF141416.toInt() else 0xFFFFFFFF.toInt(),
                        artistColor = if (isLightChassis) 0xFF4A4A52.toInt() else 0xFFA2A2AC.toInt(),
                        isLightChassis = isLightChassis
                    )
                }
                else -> { // CLASSIC_WHITE
                    WidgetThemePalette(
                        chassisTop = 0xFFF7F7F9.toInt(),
                        chassisMid = 0xFFEDEDF0.toInt(),
                        chassisBottom = 0xFFE2E2E6.toInt(),
                        chamferBorder = 0xFFC7C7CC.toInt(),
                        wheelBase = 0xFFF9F9FA.toInt(),
                        wheelOuterShade = 0xFFEDEDF0.toInt(),
                        wheelBorder = 0xFFD1D1D6.toInt(),
                        centerBase = 0xFFFFFFFF.toInt(),
                        centerOuterShade = 0xFFF2F2F4.toInt(),
                        centerBorder = 0xFFDCDCE0.toInt(),
                        wheelTextColor = 0xFF757575.toInt(),
                        songTitleColor = 0xFF1C1C1E.toInt(),
                        artistColor = 0xFF636366.toInt(),
                        isLightChassis = true
                    )
                }
            }
        }

        private fun drawChassisBitmap(palette: WidgetThemePalette, width: Int = 480, height: Int = 120): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val cornerRadius = 24f

            // 1. Chassis vertical gradient
            paint.shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(palette.chassisTop, palette.chassisMid, palette.chassisBottom),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.shader = null

            // 2. Chamfer outer border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = palette.chamferBorder
            val strokeInset = 1.25f
            val strokeRect = RectF(strokeInset, strokeInset, width - strokeInset, height - strokeInset)
            canvas.drawRoundRect(strokeRect, cornerRadius, cornerRadius, paint)

            // 3. Subtle top highlight line for authentic skeuomorphic depth
            paint.strokeWidth = 1.5f
            paint.color = if (palette.isLightChassis) 0x66FFFFFF else 0x22FFFFFF
            canvas.drawLine(cornerRadius, 2.5f, width - cornerRadius, 2.5f, paint)

            return bitmap
        }

        private fun drawWheelBitmap(palette: WidgetThemePalette, sizePx: Int = 140): Bitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val center = sizePx / 2f
            val radius = (sizePx / 2f) - 1.5f

            // 1. Outer wheel radial gradient
            paint.shader = RadialGradient(
                center, center, radius,
                intArrayOf(palette.wheelBase, palette.wheelOuterShade),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, radius, paint)
            paint.shader = null

            // 2. Wheel outer border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = palette.wheelBorder
            canvas.drawCircle(center, center, radius, paint)

            return bitmap
        }

        private fun drawCenterButtonBitmap(palette: WidgetThemePalette, sizePx: Int = 54): Bitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val center = sizePx / 2f
            val radius = (sizePx / 2f) - 1.2f

            // 1. Radial gradient for concave tactile center select button
            paint.shader = RadialGradient(
                center, center, radius,
                intArrayOf(palette.centerBase, palette.centerOuterShade),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(center, center, radius, paint)
            paint.shader = null

            // 2. Center button border rim
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.8f
            paint.color = palette.centerBorder
            canvas.drawCircle(center, center, radius, paint)

            return bitmap
        }

        private fun createDefaultRecordBitmap(
            sizePx: Int = 160,
            title: String? = null,
            artist: String? = null
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val r = sizePx / 2f
            val center = sizePx / 2f

            // 1. Rounded Cover Backdrop / Disc
            val cornerRadius = sizePx * 0.08f
            val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())

            paint.shader = RadialGradient(
                center, center, r,
                intArrayOf(0xFF222228.toInt(), 0xFF141418.toInt(), 0xFF0A0A0D.toInt()),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.shader = null

            // 2. Concentric Vinyl Grooves
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f
            val grooveCount = 8
            for (i in 3..grooveCount) {
                val grooveRadius = r * (i / (grooveCount + 1f)) * 0.92f
                paint.color = if (i % 2 == 0) 0x1AFFFFFF else 0x0DFFFFFF
                canvas.drawCircle(center, center, grooveRadius, paint)
            }

            // 3. Center Vinyl Label
            val labelRadius = r * 0.38f
            val baseLabelColor = if (title != null && artist != null) {
                val hash = (title + artist).hashCode()
                when (Math.abs(hash) % 4) {
                    0 -> 0xFF0288D1.toInt() // iPod Blue
                    1 -> 0xFFE91E63.toInt() // Magenta
                    2 -> 0xFF00897B.toInt() // Teal
                    else -> 0xFFD32F2F.toInt() // iPod Red
                }
            } else {
                0xFFD32F2F.toInt() // Classic iPod Red
            }

            // Colored record label circle
            paint.style = Paint.Style.FILL
            paint.color = baseLabelColor
            canvas.drawCircle(center, center, labelRadius, paint)

            // Subtle label border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = 0x55FFFFFF
            canvas.drawCircle(center, center, labelRadius, paint)

            // Center Spindle Hole
            val spindleRadius = r * 0.10f
            paint.style = Paint.Style.FILL
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(center, center, spindleRadius, paint)

            // Spindle Inner Rim
            paint.style = Paint.Style.FILL
            paint.color = 0xFF141416.toInt()
            canvas.drawCircle(center, center, spindleRadius * 0.75f, paint)

            // 4. Glossy Acrylic Diagonal Sheen
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                intArrayOf(0x2BFFFFFF, 0x0AFFFFFF, 0x00000000),
                floatArrayOf(0f, 0.45f, 0.85f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.shader = null

            // Subtle Outer Border
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            paint.color = 0x33FFFFFF
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            return bitmap
        }

        private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return output
        }
    }
}
