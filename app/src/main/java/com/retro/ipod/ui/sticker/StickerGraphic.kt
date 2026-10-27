package com.retro.ipod.ui.sticker

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.data.sticker.IpodSticker
import com.retro.ipod.data.sticker.StickerPresets
import com.retro.ipod.data.sticker.StickerType
import java.io.File

/**
 * Renders an authentic die-cut vinyl sticker with white decal outline and shadow.
 */
@Composable
fun StickerGraphic(
    sticker: IpodSticker,
    baseSize: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(baseSize),
        contentAlignment = Alignment.Center
    ) {
        when (sticker.type) {
            StickerType.ASSET_IMAGE -> {
                sticker.assetPath?.let { AssetImageStickerView(assetPath = it) }
            }
            StickerType.CUSTOM_GALLERY -> {
                CustomImageStickerView(filePath = sticker.customImagePath)
            }
            StickerType.PRESET_VECTOR -> {
                when (sticker.presetId) {
                    StickerPresets.APPLE_RAINBOW -> AppleRainbowSticker()
                    StickerPresets.HI_RES_GOLD -> HiResGoldSticker()
                    StickerPresets.PARENTAL_ADVISORY -> ParentalAdvisorySticker()
                    StickerPresets.CASSETTE_TAPE -> CassetteTapeSticker()
                    StickerPresets.VINYL_RECORD -> VinylRecordSticker()
                    StickerPresets.PIXEL_HEART -> PixelHeartSticker()
                    StickerPresets.ACID_SMILEY -> AcidSmileySticker()
                    StickerPresets.RETRO_FLAME -> RetroFlameSticker()
                    else -> AppleRainbowSticker()
                }
            }
        }
    }
}

@Composable
fun AssetImageStickerView(assetPath: String) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        try {
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Asset Sticker",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun CustomImageStickerView(filePath: String?) {
    val bitmap = remember(filePath) {
        if (!filePath.isNullOrBlank() && File(filePath).exists()) {
            try {
                BitmapFactory.decodeFile(filePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFF0F0F0), RoundedCornerShape(10.dp))
            .padding(2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Custom Sticker",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("PHOTO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}

@Composable
fun AppleRainbowSticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 6 retro Apple rainbow stripe colors
            val stripes = listOf(
                Color(0xFF61BB46), // Green
                Color(0xFFFDB827), // Yellow
                Color(0xFFF5821F), // Orange
                Color(0xFFE03A3E), // Red
                Color(0xFF963D97), // Purple
                Color(0xFF009DDC)  // Blue
            )

            val stripeHeight = h * 0.75f / stripes.size
            val startY = h * 0.22f

            stripes.forEachIndexed { i, c ->
                drawRect(
                    color = c,
                    topLeft = Offset(w * 0.12f, startY + (i * stripeHeight)),
                    size = Size(w * 0.76f, stripeHeight + 0.5f)
                )
            }

            // Leaf on top
            val leafPath = Path().apply {
                moveTo(w * 0.52f, h * 0.08f)
                quadraticBezierTo(w * 0.70f, h * 0.08f, w * 0.65f, h * 0.20f)
                quadraticBezierTo(w * 0.48f, h * 0.20f, w * 0.52f, h * 0.08f)
                close()
            }
            drawPath(leafPath, color = Color(0xFF61BB46))

            // Apple bite cutout on right
            drawCircle(
                color = Color.White,
                radius = w * 0.16f,
                center = Offset(w * 0.88f, h * 0.55f)
            )
        }
    }
}

@Composable
fun HiResGoldSticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF141416))
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFE082), Color(0xFFFF8F00))
                    ),
                    RoundedCornerShape(4.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Hi-Res",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "AUDIO",
                    color = Color(0xFFFFE082),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

@Composable
fun ParentalAdvisorySticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFF101010), RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PARENTAL",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ADVISORY",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.8.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(1.dp)
                    .background(Color.Black)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "EXPLICIT CONTENT",
                color = Color.Black,
                fontSize = 5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun CassetteTapeSticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2C2C34))
                .border(1.dp, Color(0xFF444450), RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(Color(0xFFFFF9C4), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("MIX '05 ★", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
                }
                Spacer(modifier = Modifier.height(5.dp))
                // Tape Spools Window
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF18181C)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color.LightGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.weight(1.5f))
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color.LightGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun VinylRecordSticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color.White)
            .border(2.5.dp, Color.White, CircleShape)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Outer vinyl disc
            drawCircle(color = Color(0xFF141416), radius = r, center = center)

            // Grooves
            val grooveColors = listOf(Color(0xFF24242A), Color(0xFF1E1E22))
            for (step in 3..9) {
                val gr = r * (step / 11f)
                drawCircle(
                    color = grooveColors[step % 2],
                    radius = gr,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // Center Record Label
            drawCircle(
                color = Color(0xFFD32F2F),
                radius = r * 0.36f,
                center = center
            )
            // Center Spindle Hole
            drawCircle(
                color = Color.White,
                radius = r * 0.10f,
                center = center
            )
        }
    }
}

@Composable
fun PixelHeartSticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val heartPath = Path().apply {
                moveTo(w * 0.5f, h * 0.85f)
                lineTo(w * 0.15f, h * 0.45f)
                lineTo(w * 0.15f, h * 0.28f)
                lineTo(w * 0.32f, h * 0.15f)
                lineTo(w * 0.50f, h * 0.32f)
                lineTo(w * 0.68f, h * 0.15f)
                lineTo(w * 0.85f, h * 0.28f)
                lineTo(w * 0.85f, h * 0.45f)
                close()
            }
            drawPath(
                path = heartPath,
                brush = Brush.verticalGradient(listOf(Color(0xFFFF1744), Color(0xFFC2185B)))
            )
            // Pixel specular highlight
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(w * 0.28f, h * 0.26f),
                size = Size(w * 0.12f, h * 0.12f)
            )
        }
    }
}

@Composable
fun AcidSmileySticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Color(0xFFEEEEEE), CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFFFFEA00))
                .border(2.dp, Color.Black, CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Eyes (black vertical ovals)
                drawOval(
                    color = Color.Black,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.14f, h * 0.24f)
                )
                drawOval(
                    color = Color.Black,
                    topLeft = Offset(w * 0.58f, h * 0.28f),
                    size = Size(w * 0.14f, h * 0.24f)
                )

                // Big grinning mouth
                val mouth = Path().apply {
                    moveTo(w * 0.22f, h * 0.58f)
                    quadraticBezierTo(w * 0.50f, h * 0.88f, w * 0.78f, h * 0.58f)
                }
                drawPath(mouth, color = Color.Black, style = Stroke(width = 4.5f))
            }
        }
    }
}

@Composable
fun RetroFlameSticker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(2.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Outer flame (Red)
            val outerPath = Path().apply {
                moveTo(w * 0.5f, h * 0.05f)
                cubicTo(w * 0.85f, h * 0.35f, w * 0.95f, h * 0.70f, w * 0.5f, h * 0.95f)
                cubicTo(w * 0.05f, h * 0.70f, w * 0.15f, h * 0.35f, w * 0.5f, h * 0.05f)
                close()
            }
            drawPath(outerPath, color = Color(0xFFD50000))

            // Middle flame (Orange)
            val midPath = Path().apply {
                moveTo(w * 0.5f, h * 0.28f)
                cubicTo(w * 0.75f, h * 0.48f, w * 0.82f, h * 0.76f, w * 0.5f, h * 0.94f)
                cubicTo(w * 0.18f, h * 0.76f, w * 0.25f, h * 0.48f, w * 0.5f, h * 0.28f)
                close()
            }
            drawPath(midPath, color = Color(0xFFFF6D00))

            // Inner flame (Yellow)
            val innerPath = Path().apply {
                moveTo(w * 0.5f, h * 0.50f)
                cubicTo(w * 0.65f, h * 0.65f, w * 0.70f, h * 0.82f, w * 0.5f, h * 0.92f)
                cubicTo(w * 0.30f, h * 0.82f, w * 0.35f, h * 0.65f, w * 0.5f, h * 0.50f)
                close()
            }
            drawPath(innerPath, color = Color(0xFFFFEA00))
        }
    }
}
