package com.retro.ipod.data.sticker

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages sticker state and persistence on the iPod hardware chassis.
 * Enforces a strict maximum of 3 active stickers.
 */
class StickersRepository(private val context: Context) {

    companion object {
        private const val TAG = "StickersRepository"
        private const val STICKERS_FILE = "stickers.json"
        const val MAX_STICKERS = 3

        // Visible default spawn positions on the front faceplate between the screen and click wheel
        val DEFAULT_INITIAL_POSITIONS = listOf(
            0.22f to 0.48f, // Left space between screen & wheel
            0.78f to 0.48f, // Right space between screen & wheel
            0.50f to 0.48f  // Center space between screen & wheel
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _stickersFlow = MutableStateFlow<List<IpodSticker>>(emptyList())
    val stickersFlow: StateFlow<List<IpodSticker>> = _stickersFlow.asStateFlow()

    init {
        loadStickers()
    }

    private fun getStorageFile(): File = File(context.filesDir, STICKERS_FILE)

    private fun getStickersDir(): File {
        val dir = File(context.filesDir, "stickers")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadStickers() {
        try {
            val file = getStorageFile()
            if (!file.exists()) {
                _stickersFlow.value = emptyList()
                return
            }
            val jsonText = file.readText()
            val array = JSONArray(jsonText)
            val list = mutableListOf<IpodSticker>()
            for (i in 0 until array.length().coerceAtMost(MAX_STICKERS)) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val typeStr = obj.optString("type", StickerType.PRESET_VECTOR.name)
                val type = when (typeStr) {
                    "ASSET_IMAGE" -> StickerType.ASSET_IMAGE
                    "CUSTOM_GALLERY" -> StickerType.CUSTOM_GALLERY
                    else -> StickerType.PRESET_VECTOR
                }
                val presetId = if (obj.has("presetId")) obj.getString("presetId") else null
                val assetPath = if (obj.has("assetPath")) obj.getString("assetPath") else null
                val customPath = if (obj.has("customImagePath")) obj.getString("customImagePath") else null
                val normX = obj.optDouble("normalizedX", 0.5).toFloat()
                val normY = obj.optDouble("normalizedY", 0.48).toFloat()
                val scale = obj.optDouble("scale", 1.0).toFloat().coerceIn(0.4f, 2.5f)
                val rotation = obj.optDouble("rotation", 0.0).toFloat()

                // Validate custom file existence
                if (type == StickerType.CUSTOM_GALLERY && customPath != null && !File(customPath).exists()) {
                    continue
                }

                list.add(
                    IpodSticker(
                        id = id,
                        type = type,
                        presetId = presetId,
                        assetPath = assetPath,
                        customImagePath = customPath,
                        normalizedX = normX,
                        normalizedY = normY,
                        scale = scale,
                        rotation = rotation
                    )
                )
            }
            _stickersFlow.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stickers", e)
            _stickersFlow.value = emptyList()
        }
    }

    private fun persistStickers(stickers: List<IpodSticker>) {
        try {
            val array = JSONArray()
            stickers.take(MAX_STICKERS).forEach { s ->
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("type", s.type.name)
                s.presetId?.let { obj.put("presetId", it) }
                s.assetPath?.let { obj.put("assetPath", it) }
                s.customImagePath?.let { obj.put("customImagePath", it) }
                obj.put("normalizedX", s.normalizedX)
                obj.put("normalizedY", s.normalizedY)
                obj.put("scale", s.scale)
                obj.put("rotation", s.rotation)
                array.put(obj)
            }
            getStorageFile().writeText(array.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist stickers", e)
        }
    }

    /**
     * Adds a preset sticker from PresetMeta (supports custom assets or vector presets).
     */
    fun addPresetSticker(preset: StickerPresets.PresetMeta): Boolean {
        val current = _stickersFlow.value.toMutableList()
        if (current.size >= MAX_STICKERS) return false

        val pos = DEFAULT_INITIAL_POSITIONS.getOrElse(current.size) { 0.5f to 0.48f }

        val newSticker = IpodSticker(
            id = UUID.randomUUID().toString(),
            type = preset.type,
            presetId = preset.id,
            assetPath = preset.assetPath,
            normalizedX = pos.first,
            normalizedY = pos.second,
            scale = 1.0f
        )
        current.add(newSticker)
        _stickersFlow.value = current
        scope.launch { persistStickers(current) }
        return true
    }

    /**
     * Backward-compatible helper to add preset sticker by ID.
     */
    fun addPresetSticker(presetId: String): Boolean {
        val all = StickerPresets.getAllPresets(context)
        val meta = all.firstOrNull { it.id == presetId } ?: StickerPresets.PresetMeta(
            id = presetId,
            title = "Preset",
            subtitle = "Decal",
            type = StickerType.PRESET_VECTOR
        )
        return addPresetSticker(meta)
    }

    /**
     * Imports an image from a content URI into local filesDir and adds it as a custom sticker.
     */
    fun addCustomStickerFromUri(uri: Uri): Boolean {
        val current = _stickersFlow.value.toMutableList()
        if (current.size >= MAX_STICKERS) return false

        val savedPath = try {
            val fileName = "sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
            val destFile = File(getStickersDir(), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error importing custom sticker image", e)
            null
        } ?: return false

        val pos = DEFAULT_INITIAL_POSITIONS.getOrElse(current.size) { 0.5f to 0.48f }

        val newSticker = IpodSticker(
            id = UUID.randomUUID().toString(),
            type = StickerType.CUSTOM_GALLERY,
            customImagePath = savedPath,
            normalizedX = pos.first,
            normalizedY = pos.second,
            scale = 1.0f
        )
        current.add(newSticker)
        _stickersFlow.value = current
        scope.launch { persistStickers(current) }
        return true
    }

    /**
     * Updates an existing sticker's position or scale.
     */
    fun updateSticker(updated: IpodSticker) {
        val current = _stickersFlow.value.map {
            if (it.id == updated.id) updated else it
        }
        _stickersFlow.value = current
        scope.launch { persistStickers(current) }
    }

    /**
     * Removes a single sticker by its ID.
     */
    fun removeSticker(id: String) {
        val current = _stickersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val removed = current.removeAt(index)
            removed.customImagePath?.let { path ->
                try {
                    File(path).delete()
                } catch (_: Exception) { }
            }
            _stickersFlow.value = current
            scope.launch { persistStickers(current) }
        }
    }

    /**
     * Clears all stickers from the chassis.
     */
    fun clearAllStickers() {
        val current = _stickersFlow.value
        current.forEach { s ->
            s.customImagePath?.let { path ->
                try { File(path).delete() } catch (_: Exception) { }
            }
        }
        _stickersFlow.value = emptyList()
        scope.launch { persistStickers(emptyList()) }
    }
}
