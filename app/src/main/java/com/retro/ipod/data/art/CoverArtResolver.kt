package com.retro.ipod.data.art

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.util.Size
import com.retro.ipod.IpodApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

/**
 * Universal High-Performance Cover Art Resolver.
 * Resolves artwork from:
 * 1. Embedded ID3 / FLAC / MP4 audio picture tags.
 * 2. Direct filesystem file probes for cover.jpg, folder.jpg, front.jpg, etc.
 * 3. MediaStore Audio Albums & Android Q+ loadThumbnail API.
 * 4. MediaStore Images query for images in the album directory.
 */
object CoverArtResolver {

    private const val TAG = "CoverArtResolver"
    private val memoryCache = LruCache<String, Bitmap>(40)
    private val folderArtCache = LruCache<String, File>(150)
    private val negativeFolderCache = LruCache<String, Boolean>(150)
    private val notificationBytesCache = LruCache<String, ByteArray>(30)

    private val DIRECT_NAMES = listOf(
        "cover.jpg", "Cover.jpg", "COVER.JPG",
        "cover.png", "Cover.png", "COVER.PNG",
        "cover.jpeg", "Cover.jpeg", "COVER.JPEG",
        "cover.webp", "Cover.webp", "COVER.WEBP",
        "folder.jpg", "Folder.jpg", "FOLDER.JPG",
        "folder.png", "Folder.png", "FOLDER.PNG",
        "folder.jpeg", "Folder.jpeg", "FOLDER.JPEG",
        "front.jpg", "Front.jpg", "FRONT.JPG",
        "front.png", "Front.png", "FRONT.PNG",
        "album.jpg", "Album.jpg", "ALBUM.JPG",
        "album.png", "Album.png", "ALBUM.PNG",
        "artwork.jpg", "Artwork.jpg", "ARTWORK.JPG"
    )

    /**
     * Resolves an album cover image file in the audio file's directory (or parent directory).
     * Uses direct file testing without relying exclusively on Scoped-Storage-restricted dir.listFiles().
     */
    fun findCoverArtFile(audioFilePath: String?): File? {
        if (audioFilePath.isNullOrBlank()) return null
        val cleanPath = audioFilePath.replace('\\', '/')
        val parentPath = cleanPath.substringBeforeLast('/', "")
        if (parentPath.isBlank()) return null

        synchronized(folderArtCache) {
            val cached = folderArtCache.get(parentPath)
            if (cached != null) return cached
            if (negativeFolderCache.get(parentPath) == true) return null
        }

        val parentDir = File(parentPath)
        if (!parentDir.exists() || !parentDir.isDirectory) {
            synchronized(folderArtCache) { negativeFolderCache.put(parentPath, true) }
            return null
        }
        val grandParentDir = parentDir.parentFile

        // 1. Direct file probes on parent directory
        for (name in DIRECT_NAMES) {
            val candidate = File(parentDir, name)
            if (candidate.exists() && candidate.canRead()) {
                synchronized(folderArtCache) { folderArtCache.put(parentPath, candidate) }
                return candidate
            }
        }

        // 2. Direct file probes on grandparent directory (for CD 1 / Disc 1 subfolders)
        if (grandParentDir != null && grandParentDir.exists() && grandParentDir.isDirectory) {
            for (name in DIRECT_NAMES) {
                val candidate = File(grandParentDir, name)
                if (candidate.exists() && candidate.canRead()) {
                    synchronized(folderArtCache) { folderArtCache.put(parentPath, candidate) }
                    return candidate
                }
            }
        }

        // 3. Directory listing fallback if filesystem permissions permit
        try {
            val files = parentDir.listFiles()
            if (files != null) {
                val match = files.firstOrNull { file ->
                    if (!file.isFile) return@firstOrNull false
                    val ext = file.extension.lowercase()
                    if (ext !in listOf("jpg", "jpeg", "png", "webp")) return@firstOrNull false
                    val nameWithoutExt = file.nameWithoutExtension.lowercase()
                    nameWithoutExt.contains("cover") ||
                    nameWithoutExt.contains("folder") ||
                    nameWithoutExt.contains("front") ||
                    nameWithoutExt.contains("album") ||
                    nameWithoutExt.contains("art")
                }
                if (match != null && match.canRead()) {
                    synchronized(folderArtCache) { folderArtCache.put(parentPath, match) }
                    return match
                }

                // Any image in the folder
                val anyImg = files.firstOrNull { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
                if (anyImg != null && anyImg.canRead()) {
                    synchronized(folderArtCache) { folderArtCache.put(parentPath, anyImg) }
                    return anyImg
                }
            }
        } catch (_: Exception) {}

        synchronized(folderArtCache) {
            negativeFolderCache.put(parentPath, true)
        }
        return null
    }

    /**
     * Extracts raw embedded picture bytes from audio tags if present.
     */
    fun getEmbeddedPictureBytes(audioFilePath: String?): ByteArray? {
        if (audioFilePath.isNullOrBlank()) return null
        val file = try { File(audioFilePath) } catch (_: Exception) { return null }
        if (!file.exists()) return null

        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val bytes = retriever.embeddedPicture
            retriever.release()
            bytes
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Queries MediaStore for track thumbnail or album art.
     */
    private fun queryMediaStoreForCover(audioFilePath: String, maxDimension: Int): Bitmap? {
        val context = try { IpodApplication.instance } catch (_: Exception) { return null }
        val contentResolver = context.contentResolver

        // Method A: Query MediaStore.Audio.Media for ALBUM_ID or _ID
        try {
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.DATA} = ?"
            val selectionArgs = arrayOf(audioFilePath)

            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val songId = cursor.getLong(0)
                    val albumId = cursor.getLong(1)

                    // On Android Q+ (API 29+), loadThumbnail from track URI or album URI
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val trackUri = ContentUris.withAppendedId(collection, songId)
                            val bmp = contentResolver.loadThumbnail(trackUri, Size(maxDimension, maxDimension), null)
                            if (bmp != null) return bmp
                        } catch (_: Exception) {}

                        try {
                            val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL), albumId)
                            val bmp = contentResolver.loadThumbnail(albumUri, Size(maxDimension, maxDimension), null)
                            if (bmp != null) return bmp
                        } catch (_: Exception) {}
                    }

                    // Content albumart URI fallback
                    try {
                        val artUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                        contentResolver.openInputStream(artUri)?.use { input ->
                            val bmp = BitmapFactory.decodeStream(input)
                            if (bmp != null) return bmp
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore audio query failed", e)
        }

        // Method B: Query MediaStore.Images.Media for images located in the same directory
        try {
            val cleanPath = audioFilePath.replace('\\', '/')
            val parentPath = cleanPath.substringBeforeLast('/', "")
            if (parentPath.isNotBlank()) {
                val imgCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                val imgProj = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
                )
                val imgSel = "${MediaStore.Images.Media.DATA} LIKE ?"
                val imgArgs = arrayOf("$parentPath/%")
                contentResolver.query(imgCollection, imgProj, imgSel, imgArgs, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    var bestId: Long? = null
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: ""
                        if (name.startsWith("cover", ignoreCase = true) ||
                            name.startsWith("folder", ignoreCase = true) ||
                            name.startsWith("front", ignoreCase = true)) {
                            bestId = id
                            break
                        }
                        if (bestId == null && (name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true))) {
                            bestId = id
                        }
                    }
                    if (bestId != null) {
                        val imgUri = ContentUris.withAppendedId(imgCollection, bestId)
                        contentResolver.openInputStream(imgUri)?.use { input ->
                            val bmp = BitmapFactory.decodeStream(input)
                            if (bmp != null) return bmp
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore image query failed", e)
        }

        return null
    }

    /**
     * Loads a high-performance Bitmap for the track, checking:
     * 1. Memory LruCache
     * 2. Embedded ID3 picture
     * 3. Direct folder cover art file (cover.jpg, folder.jpg, etc.)
     * 4. MediaStore album art / loadThumbnail API
     */
    suspend fun loadBitmap(audioFilePath: String?, maxDimension: Int = 1024): Bitmap? = withContext(Dispatchers.IO) {
        if (audioFilePath.isNullOrBlank()) return@withContext null

        synchronized(memoryCache) {
            val cached = memoryCache.get(audioFilePath)
            if (cached != null && !cached.isRecycled) return@withContext cached
        }

        // 1. Check embedded picture
        val embeddedBytes = getEmbeddedPictureBytes(audioFilePath)
        var bitmap: Bitmap? = null
        if (embeddedBytes != null) {
            bitmap = decodeSampledBitmap(embeddedBytes, maxDimension)
        }

        // 2. Direct filesystem file check (cover.jpg, etc.)
        if (bitmap == null) {
            val coverFile = findCoverArtFile(audioFilePath)
            if (coverFile != null && coverFile.exists()) {
                bitmap = decodeSampledBitmap(coverFile, maxDimension)
            }
        }

        // 3. MediaStore ContentProvider fallback (Android Q+ loadThumbnail & album art)
        if (bitmap == null) {
            bitmap = queryMediaStoreForCover(audioFilePath, maxDimension)
        }

        if (bitmap != null) {
            synchronized(memoryCache) {
                memoryCache.put(audioFilePath, bitmap)
            }
        }

        return@withContext bitmap
    }

    /**
     * Synchronous / fast retrieval for notification payload.
     * Should be called from a background thread (e.g. Dispatchers.IO).
     */
    fun getNotificationArtworkBytes(audioFilePath: String?): ByteArray? {
        if (audioFilePath.isNullOrBlank()) return null

        synchronized(notificationBytesCache) {
            val cachedBytes = notificationBytesCache.get(audioFilePath)
            if (cachedBytes != null) return cachedBytes
        }

        synchronized(memoryCache) {
            val cached = memoryCache.get(audioFilePath)
            if (cached != null && !cached.isRecycled) {
                val stream = ByteArrayOutputStream()
                cached.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()
                synchronized(notificationBytesCache) { notificationBytesCache.put(audioFilePath, bytes) }
                return bytes
            }
        }

        // 1. Direct file check first (fast with directory cache)
        val file = findCoverArtFile(audioFilePath)
        if (file != null && file.exists()) {
            val bmp = decodeSampledBitmap(file, 512)
            if (bmp != null) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()
                synchronized(notificationBytesCache) { notificationBytesCache.put(audioFilePath, bytes) }
                return bytes
            }
        }

        // 2. Embedded ID3 / FLAC picture bytes
        val embeddedBytes = getEmbeddedPictureBytes(audioFilePath)
        if (embeddedBytes != null) {
            val bmp = decodeSampledBitmap(embeddedBytes, 512)
            if (bmp != null) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                val bytes = stream.toByteArray()
                synchronized(notificationBytesCache) { notificationBytesCache.put(audioFilePath, bytes) }
                return bytes
            }
        }

        // 3. MediaStore
        val bmp = queryMediaStoreForCover(audioFilePath, 512)
        if (bmp != null) {
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()
            synchronized(notificationBytesCache) { notificationBytesCache.put(audioFilePath, bytes) }
            return bytes
        }

        return null
    }

    /**
     * Gets a file or content Uri if an external cover image is present.
     */
    fun getArtworkUri(audioFilePath: String?): Uri? {
        val file = findCoverArtFile(audioFilePath)
        if (file != null) return Uri.fromFile(file)
        return null
    }

    private fun decodeSampledBitmap(bytes: ByteArray, maxDimension: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(file: File, maxDimension: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            FileInputStream(file).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            FileInputStream(file).use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
