package com.retro.ipod.data.sticker

import android.content.Context

/**
 * Represents a draggable, resizable vinyl/decal sticker placed on the iPod hardware chassis.
 * Maximum of 3 stickers can be active simultaneously.
 */
data class IpodSticker(
    val id: String,
    val type: StickerType,
    val presetId: String? = null,
    val assetPath: String? = null, // e.g. "images/stickers/headphone.png"
    val customImagePath: String? = null,
    val normalizedX: Float = 0.22f, // 0.0f .. 1.0f (percentage across chassis width)
    val normalizedY: Float = 0.48f, // 0.0f .. 1.0f (percentage across chassis height - between screen and wheel)
    val scale: Float = 1.0f, // 0.4f .. 2.5f
    val rotation: Float = 0f
)

enum class StickerType {
    ASSET_IMAGE,
    PRESET_VECTOR,
    CUSTOM_GALLERY
}

/**
 * Preset stickers collection including custom image pack assets from images/stickers and classic decals.
 */
object StickerPresets {
    val APPLE_RAINBOW = "preset_apple"
    val HI_RES_GOLD = "preset_hires"
    val PARENTAL_ADVISORY = "preset_parental"
    val CASSETTE_TAPE = "preset_cassette"
    val VINYL_RECORD = "preset_vinyl"
    val PIXEL_HEART = "preset_heart"
    val ACID_SMILEY = "preset_smiley"
    val RETRO_FLAME = "preset_flame"

    data class PresetMeta(
        val id: String,
        val title: String,
        val subtitle: String,
        val assetPath: String? = null,
        val type: StickerType = StickerType.PRESET_VECTOR
    )

    val VECTOR_PRESETS = listOf(
        PresetMeta(APPLE_RAINBOW, "Retro Rainbow Apple", "Classic 6-stripe 80s decal", type = StickerType.PRESET_VECTOR),
        PresetMeta(HI_RES_GOLD, "Hi-Res Audio Gold", "Audiophile certified gold seal", type = StickerType.PRESET_VECTOR),
        PresetMeta(PARENTAL_ADVISORY, "Parental Advisory", "Explicit content album sticker", type = StickerType.PRESET_VECTOR),
        PresetMeta(CASSETTE_TAPE, "Mixtape Cassette", "Translucent vintage cassette", type = StickerType.PRESET_VECTOR),
        PresetMeta(VINYL_RECORD, "Grooved Vinyl LP", "Classic vinyl disc with red label", type = StickerType.PRESET_VECTOR),
        PresetMeta(PIXEL_HEART, "8-Bit Arcade Heart", "Chunky pixel art red heart", type = StickerType.PRESET_VECTOR),
        PresetMeta(ACID_SMILEY, "90s Acid Smiley", "Rave culture yellow smiley", type = StickerType.PRESET_VECTOR),
        PresetMeta(RETRO_FLAME, "Hot Rod Street Flame", "Orange-red street decal", type = StickerType.PRESET_VECTOR)
    )

    fun getAssetPresets(context: Context): List<PresetMeta> {
        val list = mutableListOf<PresetMeta>()
        try {
            val files = context.assets.list("images/stickers") ?: emptyArray()
            files.filter { it.endsWith(".png", true) || it.endsWith(".webp", true) || it.endsWith(".jpg", true) }
                .sorted()
                .forEach { fileName ->
                    val id = "asset_$fileName"
                    val title = when (fileName) {
                        "headphone.png" -> "Pixel Headphones"
                        "heart-pixe.png" -> "8-Bit Heart"
                        "music-note-1.png" -> "Pixel Music Note"
                        "sparkles.png" -> "Pixel Sparkles"
                        "star-patch.png" -> "Stitched Star Patch"
                        "stars.png" -> "Red Star Scatter"
                        else -> fileName.substringBeforeLast('.').replace('-', ' ').replace('_', ' ')
                            .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    }
                    list.add(
                        PresetMeta(
                            id = id,
                            title = title,
                            subtitle = "Asset Sticker Pack",
                            assetPath = "images/stickers/$fileName",
                            type = StickerType.ASSET_IMAGE
                        )
                    )
                }
        } catch (_: Exception) { }
        return list
    }

    fun getAllPresets(context: Context): List<PresetMeta> {
        return getAssetPresets(context) + VECTOR_PRESETS
    }
}
