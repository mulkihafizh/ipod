package com.retro.ipod.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retro.ipod.playback.audio.NativeAudioBridge
import com.retro.ipod.playback.audio.UsbDacState
import com.retro.ipod.ui.theme.IpodScreenPalette

/**
 * Native in-screen USB DAC Hardware Inspector adhering to the iPod skeuomorphic LCD layout.
 * Replaces external Android modal dialogs with an integrated inside-the-screen experience.
 */
@Composable
fun UsbDacDetailsScreen(
    dacState: UsbDacState,
    palette: IpodScreenPalette,
    onToggleExclusive: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.screenBackground)
            .border(1.dp, palette.screenBorder, RoundedCornerShape(8.dp))
    ) {
        // Title Bar with Back action
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(palette.titleBarBrush)
                .clickable { onBack() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "‹",
                    color = palette.chevronColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = "Audio Hardware Details",
                    color = palette.titleBarText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.dividerColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(palette.menuLeftBackground)
                .verticalScroll(scrollState)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hero Route Card
            val isBitPerfect = dacState.isClaimed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.badgeBackground)
                    .border(1.dp, palette.headerBarBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isBitPerfect) Color(0xFF34C759) else Color(0xFFFF9500))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isBitPerfect) "BIT-PERFECT USB DAC" else "ANDROID DIRECT HAL",
                                color = palette.itemText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBitPerfect) "High-Resolution Bit-Exact Pipeline" else "Direct Device Output",
                            color = palette.itemSubtitle,
                            fontSize = 8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(palette.selectionGradient)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${dacState.lockedSampleRateKhz}kHz / ${dacState.lockedBitDepth}bit",
                            color = palette.selectionText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hardware Specs Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.screenBackground)
                    .border(0.5.dp, palette.dividerColor, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DEVICE ATTRIBUTES",
                    color = palette.settingsItemValue,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )

                val deviceName = if (dacState.deviceName.isNotBlank() &&
                    !dacState.deviceName.equals("DIRECT", ignoreCase = true) &&
                    !dacState.deviceName.equals("Internal DAC", ignoreCase = true)
                ) {
                    dacState.deviceName
                } else {
                    "DIRECT (Internal Audio)"
                }
                DacSpecRow("Device Name", deviceName, palette)

                if (dacState.vendorId != 0) {
                    val vid = "0x" + dacState.vendorId.toString(16).uppercase()
                    val pid = "0x" + dacState.productId.toString(16).uppercase()
                    DacSpecRow("USB VID / PID", "$vid | $pid", palette)
                }

                val ifaceText = if (dacState.hasAudioStreamingInterface) "UAC 2.0 Async (Detected)" else "Standard AudioTrack"
                DacSpecRow("Streaming", ifaceText, palette)

                val engineText = if (NativeAudioBridge.isLoaded()) "Oboe ${NativeAudioBridge.getOboeVersion()} (C++)" else "Direct High-Res HAL"
                DacSpecRow("Engine", engineText, palette)
            }

            // Bit-Perfect Exclusive Mode Switch Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.screenBackground)
                    .border(0.5.dp, palette.dividerColor, RoundedCornerShape(6.dp))
                    .clickable { onToggleExclusive(!dacState.isExclusiveModeEnabled) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bit-Perfect Exclusive",
                        color = palette.itemText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bypass Android AudioFlinger mixer & resampler",
                        color = palette.itemSubtitle,
                        fontSize = 8.sp
                    )
                }

                Switch(
                    checked = dacState.isExclusiveModeEnabled,
                    onCheckedChange = { onToggleExclusive(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.settingsItemValue,
                        checkedTrackColor = palette.settingsItemValue.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Press MENU on Click Wheel to return",
                color = palette.itemSubtitle,
                fontSize = 8.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DacSpecRow(label: String, value: String, palette: IpodScreenPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = palette.itemSubtitle, fontSize = 9.sp)
        Text(
            text = value,
            color = palette.itemText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

