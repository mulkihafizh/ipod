package com.retro.ipod

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import com.retro.ipod.data.db.entity.PlaylistEntity
import com.retro.ipod.data.db.entity.PlaylistSongCrossRef
import com.retro.ipod.data.db.entity.SongEntity
import com.retro.ipod.data.playlist.M3uPlaylistManager
import com.retro.ipod.data.settings.BgMode
import com.retro.ipod.data.settings.LyricsDisplayMode
import com.retro.ipod.data.settings.SkinTheme
import com.retro.ipod.lyrics.model.LyricsDocument
import com.retro.ipod.lyrics.repository.LyricsRepository
import com.retro.ipod.playback.MusicPlayerManager
import com.retro.ipod.playback.audio.NativeAudioBridge
import com.retro.ipod.playback.audio.UsbAudioDeviceManager
import com.retro.ipod.playback.audio.UsbDacState
import com.retro.ipod.data.sticker.IpodSticker
import com.retro.ipod.data.sticker.StickerPresets
import com.retro.ipod.data.sticker.StickersRepository
import com.retro.ipod.ui.chassis.IpodChassis
import com.retro.ipod.ui.lyrics.LyricsFlowScreen
import com.retro.ipod.ui.navigation.IpodMenuItem
import com.retro.ipod.ui.navigation.IpodScreen
import com.retro.ipod.ui.sticker.StickerGraphic
import com.retro.ipod.ui.sticker.StickersLayer
import com.retro.ipod.ui.theme.AppFonts
import com.retro.ipod.ui.nowplaying.IpodNowPlayingScreen
import com.retro.ipod.ui.search.SearchScreen
import com.retro.ipod.ui.settings.IpodSettingsListScreen
import com.retro.ipod.ui.settings.SettingsRowItem
import com.retro.ipod.ui.split.IpodSplitMenuScreen
import com.retro.ipod.ui.wheel.ClickWheel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen Immersive Mode: Completely hide system status bar (top notification) and navigation bar (home, back, history)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        val app = application as IpodApplication
        val dao = app.database.musicNavigationDao()
        val playlistDao = app.database.playlistDao()
        val scanner = app.metadataScanner
        val settingsRepo = app.settingsRepository
        val playerManager = app.playerManager
        val usbDacManager = app.usbAudioDeviceManager

        setContent {
            MaterialTheme(
                typography = AppFonts.ipodTypography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0E)
                ) {
                    IpodMasterApp(
                        dao = dao,
                        playlistDao = playlistDao,
                        scanner = scanner,
                        database = app.database,
                        settingsRepo = settingsRepo,
                        playerManager = playerManager,
                        usbDacManager = usbDacManager
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

data class DeviceBatteryState(
    val level: Int = 100,
    val isCharging: Boolean = false
)

@Composable
fun rememberBatteryState(): DeviceBatteryState {
    val context = LocalContext.current
    var batteryState by remember {
        val initialIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = initialIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = initialIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        val status = initialIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        mutableStateOf(DeviceBatteryState(level = pct, isCharging = isCharging))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    batteryState = DeviceBatteryState(level = pct, isCharging = isCharging)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) { }
        }
    }

    return batteryState
}

@Composable
fun IpodMasterApp(
    dao: com.retro.ipod.data.db.dao.MusicNavigationDao,
    playlistDao: com.retro.ipod.data.db.dao.PlaylistDao,
    scanner: com.retro.ipod.data.scanner.AudioMetadataScanner,
    database: com.retro.ipod.data.db.IpodDatabase,
    settingsRepo: com.retro.ipod.data.settings.AppSettingsRepository,
    playerManager: MusicPlayerManager,
    usbDacManager: UsbAudioDeviceManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Data streams from Room & DataStore
    val songs by dao.getAllSongs().collectAsState(initial = emptyList())
    val artists by dao.getAllArtists().collectAsState(initial = emptyList())
    val albums by dao.getAllAlbums().collectAsState(initial = emptyList())
    val playlists by playlistDao.getAllPlaylists().collectAsState(initial = emptyList())
    val settings by settingsRepo.appSettingsFlow.collectAsState(initial = null)

    val playbackState by playerManager.playbackState.collectAsState()
    val highPrecisionPositionMs by playerManager.playbackPositionMs.collectAsState()
    val dacState by usbDacManager.dacState.collectAsState()
    val batteryState = rememberBatteryState()

    // Modals & Navigation Stack State
    var showDacModal by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<IpodScreen>(IpodScreen.MainMenu) }
    val backStack = remember { mutableStateListOf<IpodScreen>() }

    // Highlighted indexes map to preserve scroll position across drill-down menus
    val highlightMap = remember { mutableMapOf<String, Int>() }
    var activeHighlightedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    var activeSelectAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var activeItemCount by remember { mutableIntStateOf(0) }

    // Helper functions for navigation
    fun navigateTo(screen: IpodScreen) {
        val currentKey = currentScreen.javaClass.simpleName
        highlightMap[currentKey] = activeHighlightedIndex
        backStack.add(currentScreen)
        currentScreen = screen
        val newKey = screen.javaClass.simpleName
        activeHighlightedIndex = highlightMap[newKey] ?: 0
        scope.launch { listState.scrollToItem(activeHighlightedIndex) }
    }

    fun popBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val popped = backStack.removeAt(backStack.size - 1)
            currentScreen = popped
            val key = popped.javaClass.simpleName
            activeHighlightedIndex = highlightMap[key] ?: 0
            scope.launch { listState.scrollToItem(activeHighlightedIndex) }
            return true
        }
        return false
    }

    val lyricsRepo = remember { LyricsRepository() }
    var lyricsDocument by remember { mutableStateOf(LyricsDocument()) }

    LaunchedEffect(playbackState.currentSong) {
        lyricsDocument = lyricsRepo.getLyricsForSong(playbackState.currentSong)
    }

    // Permissions Request
    val permissionsToRequest = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val hasAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasAudio) {
            launcher.launch(permissionsToRequest)
        }
    }

    var showFolderDialog by remember { mutableStateOf(false) }

    val stickersRepo = remember { StickersRepository(context) }
    val stickers by stickersRepo.stickersFlow.collectAsState()
    var isStickerEditMode by remember { mutableStateOf(false) }
    var showPresetStickerDialog by remember { mutableStateOf(false) }

    val customStickerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val added = stickersRepo.addCustomStickerFromUri(it)
            if (added) {
                Toast.makeText(context, "Added custom sticker to iPod!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Max 3 stickers allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val m3uManager = remember { M3uPlaylistManager(context, database) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var songToAddAfterCreation by remember { mutableStateOf<SongEntity?>(null) }

    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<SongEntity?>(null) }

    var showDeletePlaylistDialog by remember { mutableStateOf<PlaylistEntity?>(null) }

    // Ensure "On-The-Go" exists in database so it has a valid ID and supports foreign keys
    LaunchedEffect(playlists) {
        if (playlists.none { it.name.equals("On-The-Go", ignoreCase = true) }) {
            playlistDao.insertPlaylist(
                PlaylistEntity(name = "On-The-Go", createdAt = 0L)
            )
        }
    }

    val m3uPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            scope.launch {
                try {
                    // Strictly enforce .m3u / .m3u8 extension
                    var fileName = ""
                    context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex) ?: ""
                        }
                    }
                    if (fileName.isBlank()) {
                        fileName = fileUri.lastPathSegment ?: ""
                    }

                    if (!fileName.endsWith(".m3u", ignoreCase = true) && !fileName.endsWith(".m3u8", ignoreCase = true)) {
                        Toast.makeText(context, "Only .m3u playlist files can be loaded", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val result = m3uManager.importM3uFromUri(fileUri)
                    if (result.success) {
                        Toast.makeText(
                            context,
                            "Imported '${result.playlistName}' (${result.totalTracksFound} tracks)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "M3U Import: ${result.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "M3U Import Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { treeUri ->
            try {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            } catch (_: Exception) { }

            val resolvedPath = scanner.resolveStoragePath(treeUri)
            scope.launch {
                settingsRepo.setMusicFolder(resolvedPath, treeUri.toString())
                val folderSongs = scanner.scanFolder(resolvedPath, treeUri)
                dao.deleteAllSongs()
                dao.insertSongs(folderSongs)
            }
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = Color(0xFF1E1E26),
            title = {
                Text(
                    text = "Library Source Folder",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = if (!settings?.musicFolderPath.isNullOrBlank()) {
                            "Current folder:\n${settings?.musicFolderPath}"
                        } else {
                            "Currently scanning all device audio."
                        },
                        fontSize = 13.sp,
                        color = Color.White,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Restrict iPod library to only read tracks from a dedicated music folder.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFolderDialog = false
                        folderPickerLauncher.launch(null)
                    }
                ) {
                    Text("Choose Folder", color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (!settings?.musicFolderPath.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showFolderDialog = false
                                scope.launch {
                                    settingsRepo.setMusicFolder(null, null)
                                    val all = scanner.scanMediaStore()
                                    dao.deleteAllSongs()
                                    dao.insertSongs(all)
                                }
                            }
                        ) {
                            Text("All Storage", color = Color(0xFFE53935))
                        }
                    }
                    TextButton(onClick = { showFolderDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        )
    }

    if (showNewPlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewPlaylistDialog = false
                songToAddAfterCreation = null
            },
            containerColor = Color(0xFF1E1E26),
            title = {
                Text(
                    text = "New Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for your playlist:",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        fontFamily = AppFonts.helvetica(FontWeight.Normal)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist Name", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToCreate = newPlaylistName.trim().ifEmpty { "New Playlist" }
                        showNewPlaylistDialog = false
                        scope.launch {
                            val newId = playlistDao.insertPlaylist(
                                PlaylistEntity(
                                    name = nameToCreate,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            songToAddAfterCreation?.let { song ->
                                playlistDao.insertPlaylistSongCrossRef(
                                    PlaylistSongCrossRef(
                                        playlistId = newId,
                                        id = song.id,
                                        orderIndex = 0
                                    )
                                )
                                Toast.makeText(context, "Created '$nameToCreate' & added track", Toast.LENGTH_SHORT).show()
                                songToAddAfterCreation = null
                            } ?: run {
                                Toast.makeText(context, "Created playlist '$nameToCreate'", Toast.LENGTH_SHORT).show()
                            }
                            newPlaylistName = ""
                        }
                    }
                ) {
                    Text("Create", color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNewPlaylistDialog = false
                        songToAddAfterCreation = null
                    }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showAddToPlaylistDialog && selectedSongForPlaylist != null) {
        val targetSong = selectedSongForPlaylist!!
        AlertDialog(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                selectedSongForPlaylist = null
            },
            containerColor = Color(0xFF1E1E26),
            title = {
                Text(
                    text = "Add to Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = targetSong.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4FC3F7),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = targetSong.artist,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Action: Create New Playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2A2A35))
                            .clickable {
                                showAddToPlaylistDialog = false
                                songToAddAfterCreation = targetSong
                                newPlaylistName = ""
                                showNewPlaylistDialog = true
                            }
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistAdd,
                            contentDescription = "New Playlist",
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "+ New Playlist...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF4FC3F7)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // List of existing playlists
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(playlists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        scope.launch {
                                            val count = playlistDao.getSongCountInPlaylist(pl.playlistId)
                                            playlistDao.insertPlaylistSongCrossRef(
                                                PlaylistSongCrossRef(
                                                    playlistId = pl.playlistId,
                                                    id = targetSong.id,
                                                    orderIndex = count
                                                )
                                            )
                                            Toast.makeText(context, "Added to '${pl.name}'", Toast.LENGTH_SHORT).show()
                                        }
                                        showAddToPlaylistDialog = false
                                        selectedSongForPlaylist = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pl.name,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddToPlaylistDialog = false
                        selectedSongForPlaylist = null
                    }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    showDeletePlaylistDialog?.let { plToDelete ->
        AlertDialog(
            onDismissRequest = { showDeletePlaylistDialog = null },
            containerColor = Color(0xFF1E1E26),
            title = {
                Text(
                    text = "Delete Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${plToDelete.name}'? Tracks will remain in your library.",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    fontFamily = AppFonts.helvetica(FontWeight.Normal)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idToDelete = plToDelete.playlistId
                        showDeletePlaylistDialog = null
                        scope.launch {
                            playlistDao.deletePlaylistById(idToDelete)
                            Toast.makeText(context, "Deleted '${plToDelete.name}'", Toast.LENGTH_SHORT).show()
                        }
                        if (currentScreen is IpodScreen.PlaylistDetail && (currentScreen as IpodScreen.PlaylistDetail).playlistId == idToDelete) {
                            popBack()
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePlaylistDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showDacModal) {
        DacHardwareDialog(
            dacState = dacState,
            onDismiss = { showDacModal = false },
            onToggleExclusive = { enabled ->
                usbDacManager.toggleExclusiveMode(enabled)
            }
        )
    }

    if (showPresetStickerDialog) {
        val availablePresets = remember(context) { StickerPresets.getAllPresets(context) }
        AlertDialog(
            onDismissRequest = { showPresetStickerDialog = false },
            containerColor = Color(0xFF1E1E26),
            title = {
                Text(
                    text = "Choose Preset Sticker",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(availablePresets) { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val added = stickersRepo.addPresetSticker(preset)
                                    showPresetStickerDialog = false
                                    if (added) {
                                        Toast.makeText(context, "Applied '${preset.title}' sticker!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Max 3 stickers allowed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                StickerGraphic(
                                    sticker = IpodSticker(
                                        id = "preview_${preset.id}",
                                        type = preset.type,
                                        presetId = preset.id,
                                        assetPath = preset.assetPath
                                    ),
                                    baseSize = 42.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = preset.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = preset.subtitle,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPresetStickerDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Skeuomorphic iPod Hardware Chassis
    val activeSkin = settings?.skinTheme ?: SkinTheme.CLASSIC_WHITE
    val screenPalette = remember(activeSkin) { com.retro.ipod.ui.theme.IpodScreenPalette.forTheme(activeSkin) }

    IpodChassis(
        theme = activeSkin,
        stickersContent = {
            StickersLayer(
                stickers = stickers,
                isEditMode = isStickerEditMode,
                onUpdateSticker = { updated ->
                    stickersRepo.updateSticker(updated)
                }
            )
        },
        overlayContent = {
            if (isStickerEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xEE1E1E28))
                            .border(1.5.dp, Color(0xFF4FC3F7), RoundedCornerShape(20.dp))
                            .clickable { isStickerEditMode = false }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦ EDIT MODE: Drag • Pinch • Rotate",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0288D1))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "DONE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        },
        screenContent = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Mini Header Status Bar (Spec Section 2)
                IpodMiniHeaderBar(
                    dacState = dacState,
                    batteryState = batteryState,
                    currentSong = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    shuffleMode = playbackState.shuffleMode,
                    repeatMode = playbackState.repeatMode,
                    palette = screenPalette,
                    onDacBadgeClick = { showDacModal = true }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Screen Router
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (val screen = currentScreen) {
                        is IpodScreen.MainMenu -> {
                            val items = listOf(
                                IpodMenuItem(
                                    id = "music",
                                    title = "Music",
                                    subtitle = "${songs.size}",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.MusicMenu) }
                                ),
                                IpodMenuItem(
                                    id = "now_playing",
                                    title = "Now Playing",
                                    subtitle = playbackState.currentSong?.title ?: "Idle",
                                    artworkSongPath = playbackState.currentSong?.filePath ?: songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.NowPlayingScreen) }
                                ),
                                IpodMenuItem(
                                    id = "shuffle_songs",
                                    title = "Shuffle Songs",
                                    subtitle = if (playbackState.shuffleMode) "On" else "Off",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = {
                                        if (songs.isNotEmpty()) {
                                            if (!playbackState.shuffleMode) {
                                                playerManager.toggleShuffle()
                                            }
                                            playerManager.playSong(songs.random(), songs)
                                            navigateTo(IpodScreen.NowPlayingScreen)
                                        }
                                    }
                                ),
                                IpodMenuItem(
                                    id = "settings",
                                    title = "Settings",
                                    onClick = { navigateTo(IpodScreen.SettingsMenu) }
                                )
                            )
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "iPod",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, items.size - 1),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.MusicMenu -> {
                            val items = listOf(
                                IpodMenuItem(
                                    id = "playlists",
                                    title = "Playlists",
                                    subtitle = "${playlists.size}",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.PlaylistsList) }
                                ),
                                IpodMenuItem(
                                    id = "artists",
                                    title = "Artists",
                                    subtitle = "${artists.size}",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.ArtistsList) }
                                ),
                                IpodMenuItem(
                                    id = "albums",
                                    title = "Albums",
                                    subtitle = "${albums.size}",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.AlbumsList) }
                                ),
                                IpodMenuItem(
                                    id = "songs",
                                    title = "Songs",
                                    subtitle = "${songs.size}",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.AllSongsList) }
                                ),
                                IpodMenuItem(
                                    id = "search",
                                    title = "Search",
                                    artworkSongPath = songs.firstOrNull()?.filePath,
                                    onClick = { navigateTo(IpodScreen.SearchScreen) }
                                )
                            )
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "Music",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, items.size - 1),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.ArtistsList -> {
                            val items = artists.map { artistName ->
                                val sample = songs.firstOrNull { it.artist == artistName }
                                IpodMenuItem(
                                    id = artistName,
                                    title = artistName,
                                    artworkSongPath = sample?.filePath,
                                    onClick = { navigateTo(IpodScreen.ArtistAlbums(artistName)) }
                                )
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "Artists",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.ArtistAlbums -> {
                            val artistName = screen.artist
                            val artistSongs = songs.filter { it.artist == artistName }
                            val artistAlbums = artistSongs.map { it.album }.distinct().sorted()

                            val items = mutableListOf<IpodMenuItem>().apply {
                                add(
                                    IpodMenuItem(
                                        id = "*ALL*",
                                        title = "All Songs",
                                        subtitle = "${artistSongs.size}",
                                        artworkSongPath = artistSongs.firstOrNull()?.filePath,
                                        onClick = { navigateTo(IpodScreen.AlbumSongs(album = "*ALL*", artist = artistName)) }
                                    )
                                )
                                artistAlbums.forEach { albumName ->
                                    val sample = artistSongs.firstOrNull { it.album == albumName }
                                    add(
                                        IpodMenuItem(
                                            id = albumName,
                                            title = albumName,
                                            artworkSongPath = sample?.filePath,
                                            onClick = { navigateTo(IpodScreen.AlbumSongs(album = albumName, artist = artistName)) }
                                        )
                                    )
                                }
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = artistName,
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.AlbumsList -> {
                            val items = albums.map { albumName ->
                                val sample = songs.firstOrNull { it.album == albumName }
                                IpodMenuItem(
                                    id = albumName,
                                    title = albumName,
                                    subtitle = sample?.artist,
                                    artworkSongPath = sample?.filePath,
                                    onClick = { navigateTo(IpodScreen.AlbumSongs(album = albumName, artist = null)) }
                                )
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "Albums",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.AlbumSongs -> {
                            val targetAlbum = screen.album
                            val targetArtist = screen.artist
                            val songGroup = when {
                                targetAlbum == "*ALL*" && targetArtist != null -> songs.filter { it.artist == targetArtist }
                                targetArtist != null -> songs.filter { it.album == targetAlbum && it.artist == targetArtist }
                                else -> songs.filter { it.album == targetAlbum }
                            }.map { song ->
                                val resolvedTrack = scanner.resolveTrackNumber(song)
                                if (resolvedTrack > 0 && song.trackNumber <= 0) {
                                    song.copy(trackNumber = resolvedTrack)
                                } else song
                            }.sortedWith(
                                compareBy<SongEntity> { it.discNumber }
                                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                                    .thenBy { it.title.lowercase() }
                            )

                            val items = songGroup.map { song ->
                                val subtitleText = if (song.trackNumber > 0) {
                                    "Track ${song.trackNumber} • ${song.codec} ${song.sampleRateKhz}k"
                                } else {
                                    "${song.codec} ${song.sampleRateKhz}k"
                                }
                                IpodMenuItem(
                                    id = song.id.toString(),
                                    title = song.title,
                                    subtitle = subtitleText,
                                    hasSubMenu = false,
                                    artworkSongPath = song.filePath,
                                    onClick = {
                                        playerManager.playSong(song, songGroup)
                                        navigateTo(IpodScreen.NowPlayingScreen)
                                    }
                                )
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = if (targetAlbum == "*ALL*") targetArtist ?: "Songs" else targetAlbum,
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.AllSongsList -> {
                            val items = songs.map { song ->
                                IpodMenuItem(
                                    id = song.id.toString(),
                                    title = song.title,
                                    subtitle = "${song.artist} • ${song.codec}",
                                    hasSubMenu = false,
                                    artworkSongPath = song.filePath,
                                    onClick = {
                                        playerManager.playSong(song, songs)
                                        navigateTo(IpodScreen.NowPlayingScreen)
                                    }
                                )
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "All Songs",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.PlaylistsList -> {
                            val items = mutableListOf<IpodMenuItem>().apply {
                                add(
                                    IpodMenuItem(
                                        id = "new_playlist",
                                        title = "New Playlist...",
                                        subtitle = "Create custom playlist",
                                        artworkSongPath = songs.firstOrNull()?.filePath,
                                        hasSubMenu = false,
                                        onClick = {
                                            newPlaylistName = ""
                                            songToAddAfterCreation = null
                                            showNewPlaylistDialog = true
                                        }
                                    )
                                )
                                add(
                                    IpodMenuItem(
                                        id = "import_m3u",
                                        title = "Import M3U Playlist...",
                                        subtitle = "Select .m3u / .m3u8 file",
                                        artworkSongPath = songs.firstOrNull()?.filePath,
                                        hasSubMenu = false,
                                        onClick = {
                                            m3uPickerLauncher.launch(
                                                arrayOf(
                                                    "audio/x-mpegurl",
                                                    "audio/mpegurl",
                                                    "application/x-mpegurl",
                                                    "application/vnd.apple.mpegurl"
                                                )
                                            )
                                        }
                                    )
                                )
                                playlists.forEach { pl ->
                                    add(
                                        IpodMenuItem(
                                            id = pl.playlistId.toString(),
                                            title = pl.name,
                                            subtitle = "Playlist",
                                            artworkSongPath = songs.firstOrNull()?.filePath,
                                            onClick = {
                                                navigateTo(IpodScreen.PlaylistDetail(playlistId = pl.playlistId, name = pl.name))
                                            }
                                        )
                                    )
                                }
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "Playlists",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.PlaylistDetail -> {
                            val playlistSongs by playlistDao.getSongsForPlaylist(screen.playlistId).collectAsState(initial = emptyList())
                            val currentPl = playlists.firstOrNull { it.playlistId == screen.playlistId }

                            val items = mutableListOf<IpodMenuItem>().apply {
                                if (playlistSongs.isNotEmpty()) {
                                    add(
                                        IpodMenuItem(
                                            id = "play_all",
                                            title = "Play All",
                                            subtitle = "${playlistSongs.size} tracks",
                                            artworkSongPath = playlistSongs.firstOrNull()?.filePath,
                                            hasSubMenu = false,
                                            onClick = {
                                                playerManager.playSong(playlistSongs.first(), playlistSongs)
                                                navigateTo(IpodScreen.NowPlayingScreen)
                                            }
                                        )
                                    )
                                    add(
                                        IpodMenuItem(
                                            id = "shuffle_playlist",
                                            title = "Shuffle Playlist",
                                            subtitle = "${playlistSongs.size} tracks",
                                            artworkSongPath = playlistSongs.firstOrNull()?.filePath,
                                            hasSubMenu = false,
                                            onClick = {
                                                if (!playbackState.shuffleMode) {
                                                    playerManager.toggleShuffle()
                                                }
                                                playerManager.playSong(playlistSongs.random(), playlistSongs)
                                                navigateTo(IpodScreen.NowPlayingScreen)
                                            }
                                        )
                                    )
                                }
                                add(
                                    IpodMenuItem(
                                        id = "add_tracks",
                                        title = "Add Tracks...",
                                        subtitle = "Manage playlist tracks",
                                        artworkSongPath = songs.firstOrNull()?.filePath,
                                        onClick = {
                                            navigateTo(IpodScreen.AddSongsToPlaylist(playlistId = screen.playlistId, name = screen.name))
                                        }
                                    )
                                )
                                if (!screen.name.equals("On-The-Go", ignoreCase = true)) {
                                    add(
                                        IpodMenuItem(
                                            id = "delete_playlist",
                                            title = "Delete Playlist",
                                            subtitle = "Remove this playlist",
                                            artworkSongPath = null,
                                            hasSubMenu = false,
                                            onClick = {
                                                showDeletePlaylistDialog = currentPl ?: PlaylistEntity(playlistId = screen.playlistId, name = screen.name)
                                            }
                                        )
                                    )
                                }

                                playlistSongs.forEach { song ->
                                    add(
                                        IpodMenuItem(
                                            id = song.id.toString(),
                                            title = song.title,
                                            subtitle = "${song.artist} • ${song.codec}",
                                            hasSubMenu = false,
                                            artworkSongPath = song.filePath,
                                            onClick = {
                                                playerManager.playSong(song, playlistSongs)
                                                navigateTo(IpodScreen.NowPlayingScreen)
                                            }
                                        )
                                    )
                                }
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = screen.name,
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.AddSongsToPlaylist -> {
                            val currentPlaylistSongs by playlistDao.getSongsForPlaylist(screen.playlistId).collectAsState(initial = emptyList())
                            val currentSongIds = remember(currentPlaylistSongs) { currentPlaylistSongs.map { it.id }.toSet() }

                            val items = songs.map { song ->
                                val isInPlaylist = song.id in currentSongIds
                                IpodMenuItem(
                                    id = song.id.toString(),
                                    title = if (isInPlaylist) "✓ ${song.title}" else song.title,
                                    subtitle = if (isInPlaylist) "In Playlist • ${song.artist}" else "${song.artist} • ${song.album}",
                                    hasSubMenu = false,
                                    artworkSongPath = song.filePath,
                                    onClick = {
                                        scope.launch {
                                            if (isInPlaylist) {
                                                playlistDao.removeSongFromPlaylist(screen.playlistId, song.id)
                                                Toast.makeText(context, "Removed '${song.title}'", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val count = playlistDao.getSongCountInPlaylist(screen.playlistId)
                                                playlistDao.insertPlaylistSongCrossRef(
                                                    PlaylistSongCrossRef(
                                                        playlistId = screen.playlistId,
                                                        id = song.id,
                                                        orderIndex = count
                                                    )
                                                )
                                                Toast.makeText(context, "Added '${song.title}'", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSplitMenuScreen(
                                title = "Add to ${screen.name}",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                previewFilePath = items.getOrNull(activeHighlightedIndex)?.artworkSongPath,
                                palette = screenPalette,
                                onItemClick = { index ->
                                    activeHighlightedIndex = index
                                    items[index].onClick()
                                }
                            )
                        }

                        is IpodScreen.SearchScreen -> {
                            activeItemCount = 0
                            activeSelectAction = null

                            SearchScreen(
                                allSongs = songs,
                                onSongSelected = { song, playlist ->
                                    playerManager.playSong(song, playlist)
                                    navigateTo(IpodScreen.NowPlayingScreen)
                                },
                                onAlbumSelected = { album, artist ->
                                    navigateTo(IpodScreen.AlbumSongs(album = album, artist = artist))
                                },
                                onArtistSelected = { artist ->
                                    navigateTo(IpodScreen.ArtistAlbums(artist = artist))
                                },
                                onClose = { popBack() }
                            )
                        }

                        is IpodScreen.NowPlayingScreen -> {
                            activeItemCount = 0
                            activeSelectAction = null

                            IpodNowPlayingScreen(
                                playbackState = playbackState,
                                currentPositionMs = highPrecisionPositionMs,
                                dacState = dacState,
                                onSeekTo = { targetMs -> playerManager.seekTo(targetMs) },
                                onOpenLyrics = { navigateTo(IpodScreen.LyricsScreen) },
                                onDacBadgeClick = { showDacModal = true },
                                palette = screenPalette,
                                onToggleShuffle = { playerManager.toggleShuffle() },
                                onCycleRepeat = { playerManager.cycleRepeatMode() },
                                onAddToPlaylist = {
                                    playbackState.currentSong?.let { song ->
                                        selectedSongForPlaylist = song
                                        showAddToPlaylistDialog = true
                                    }
                                }
                            )
                        }

                        is IpodScreen.LyricsScreen -> {
                            activeItemCount = 0
                            activeSelectAction = null

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(screenPalette.screenBackground)
                                    .border(1.dp, screenPalette.screenBorder, RoundedCornerShape(8.dp))
                            ) {
                                LyricsFlowScreen(
                                    lyricsDocument = lyricsDocument,
                                    currentPositionMs = highPrecisionPositionMs,
                                    artworkFilePath = playbackState.currentSong?.filePath,
                                    lyricsMode = settings?.lyricsMode ?: LyricsDisplayMode.MODERN_FLOW,
                                    bgMode = settings?.bgMode ?: BgMode.BLURRED_COVER,
                                    glowEnabled = settings?.glowEnabled ?: true,
                                    fontScale = settings?.fontScale ?: 1.0f,
                                    lineGapDp = settings?.lineGapDp ?: 16,
                                    onSeekTo = { targetMs -> playerManager.seekTo(targetMs) },
                                    onToggleLyricsMode = {
                                        val current = settings?.lyricsMode ?: LyricsDisplayMode.MODERN_FLOW
                                        val next = if (current == LyricsDisplayMode.MODERN_FLOW) {
                                            LyricsDisplayMode.OG_CLASSIC
                                        } else {
                                            LyricsDisplayMode.MODERN_FLOW
                                        }
                                        scope.launch { settingsRepo.setLyricsMode(next) }
                                    },
                                    onCloseLyrics = { popBack() }
                                )
                            }
                        }

                        is IpodScreen.SettingsMenu -> {
                            val items = listOf(
                                SettingsRowItem(
                                    id = "shuffle",
                                    title = "Shuffle",
                                    valueLabel = if (playbackState.shuffleMode) "Songs" else "Off",
                                    onClick = { playerManager.toggleShuffle() }
                                ),
                                SettingsRowItem(
                                    id = "repeat",
                                    title = "Repeat",
                                    valueLabel = when (playbackState.repeatMode) {
                                        androidx.media3.common.Player.REPEAT_MODE_ONE -> "One"
                                        androidx.media3.common.Player.REPEAT_MODE_ALL -> "All"
                                        else -> "Off"
                                    },
                                    onClick = { playerManager.cycleRepeatMode() }
                                ),
                                SettingsRowItem(
                                    id = "audio",
                                    title = "Audio Engine",
                                    subtitle = if (dacState.isClaimed) "USB-DAC Exclusive" else "Direct HAL",
                                    onClick = { navigateTo(IpodScreen.AudioSettings) }
                                ),
                                SettingsRowItem(
                                    id = "lyrics",
                                    title = "Lyrics Engine",
                                    subtitle = if (settings?.lyricsMode == LyricsDisplayMode.MODERN_FLOW) "Modern Fluid" else "Retro OG",
                                    onClick = { navigateTo(IpodScreen.LyricsSettings) }
                                ),
                                SettingsRowItem(
                                    id = "theme",
                                    title = "Skin Theme",
                                    valueLabel = when (settings?.skinTheme) {
                                        SkinTheme.U2_EDITION -> "U2 Edition"
                                        SkinTheme.BRUSHED_ALUMINUM -> "Brushed Aluminum"
                                        else -> "Classic White"
                                    },
                                    onClick = { navigateTo(IpodScreen.ThemeSettings) }
                                ),
                                SettingsRowItem(
                                    id = "stickers",
                                    title = "iPod Stickers",
                                    subtitle = "${stickers.size}/3 active • Drag & pinch anywhere",
                                    valueLabel = if (stickers.isNotEmpty()) "${stickers.size}/3" else "None",
                                    onClick = { navigateTo(IpodScreen.StickerSettings) }
                                ),
                                SettingsRowItem(
                                    id = "haptic",
                                    title = "Haptic Click Wheel",
                                    isToggle = true,
                                    toggleValue = settings?.hapticFeedbackEnabled ?: true,
                                    onClick = {
                                        val current = settings?.hapticFeedbackEnabled ?: true
                                        scope.launch { settingsRepo.setHapticFeedbackEnabled(!current) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "music_folder",
                                    title = "Music Folder",
                                    subtitle = settings?.musicFolderPath?.let {
                                        val folderName = it.trimEnd('/', '\\').substringAfterLast('/')
                                        "Folder: $folderName"
                                    } ?: "All Device Storage",
                                    onClick = { showFolderDialog = true }
                                ),
                                SettingsRowItem(
                                    id = "rescan",
                                    title = "Rescan Library",
                                    subtitle = "${songs.size} tracks indexed",
                                    onClick = {
                                        scope.launch {
                                            val scanned = if (!settings?.musicFolderPath.isNullOrBlank() || !settings?.musicFolderUri.isNullOrBlank()) {
                                                val uri = settings?.musicFolderUri?.let { android.net.Uri.parse(it) }
                                                scanner.scanFolder(settings?.musicFolderPath, uri)
                                            } else {
                                                scanner.scanMediaStore()
                                            }
                                            dao.deleteAllSongs()
                                            dao.insertSongs(scanned)
                                        }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "import_m3u_settings",
                                    title = "Import M3U Playlist",
                                    subtitle = "Load .m3u / .m3u8 from storage",
                                    onClick = {
                                        m3uPickerLauncher.launch(
                                            arrayOf(
                                                "audio/x-mpegurl",
                                                "audio/mpegurl",
                                                "application/x-mpegurl",
                                                "application/vnd.apple.mpegurl"
                                            )
                                        )
                                    }
                                )
                            )
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSettingsListScreen(
                                title = "Settings",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, items.size - 1),
                                listState = listState,
                                palette = screenPalette
                            )
                        }

                        is IpodScreen.AudioSettings -> {
                            val items = listOf(
                                SettingsRowItem(
                                    id = "exclusive",
                                    title = "Direct USB Exclusive",
                                    subtitle = "Bypass Android AudioFlinger",
                                    isToggle = true,
                                    toggleValue = dacState.isExclusiveModeEnabled,
                                    onClick = {
                                        val next = !dacState.isExclusiveModeEnabled
                                        usbDacManager.toggleExclusiveMode(next)
                                        scope.launch { settingsRepo.setUsbExclusiveMode(next) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "bitperfect",
                                    title = "Bit-Perfect Stream Alignment",
                                    isToggle = true,
                                    toggleValue = settings?.bitPerfectOutput ?: true,
                                    onClick = {
                                        val current = settings?.bitPerfectOutput ?: true
                                        scope.launch { settingsRepo.setBitPerfectOutput(!current) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "dac_modal",
                                    title = "USB DAC Hardware Info",
                                    valueLabel = dacState.statusBadgeText,
                                    onClick = { showDacModal = true }
                                )
                            )
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSettingsListScreen(
                                title = "Audio Engine",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, items.size - 1),
                                listState = listState,
                                palette = screenPalette
                            )
                        }

                        is IpodScreen.LyricsSettings -> {
                            val currentMode = settings?.lyricsMode ?: LyricsDisplayMode.MODERN_FLOW
                            val currentBg = settings?.bgMode ?: BgMode.GRADIENT_MESH
                            val currentGlow = settings?.glowEnabled ?: true

                            val items = listOf(
                                SettingsRowItem(
                                    id = "style",
                                    title = "Display Style",
                                    valueLabel = if (currentMode == LyricsDisplayMode.MODERN_FLOW) "Modern Fluid" else "Retro OG",
                                    onClick = {
                                        val next = if (currentMode == LyricsDisplayMode.MODERN_FLOW) LyricsDisplayMode.OG_CLASSIC else LyricsDisplayMode.MODERN_FLOW
                                        scope.launch { settingsRepo.setLyricsMode(next) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "bg",
                                    title = "Background Effect",
                                    valueLabel = if (currentBg == BgMode.GRADIENT_MESH) "Aurora Mesh" else "Blurred Art",
                                    onClick = {
                                        val next = if (currentBg == BgMode.GRADIENT_MESH) BgMode.BLURRED_COVER else BgMode.GRADIENT_MESH
                                        scope.launch { settingsRepo.setBgMode(next) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "glow",
                                    title = "Dynamic Glow Bloom",
                                    isToggle = true,
                                    toggleValue = currentGlow,
                                    onClick = {
                                        scope.launch { settingsRepo.setGlowEnabled(!currentGlow) }
                                    }
                                )
                            )
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSettingsListScreen(
                                title = "Lyrics Engine",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, items.size - 1),
                                listState = listState,
                                palette = screenPalette
                            )
                        }

                        is IpodScreen.ThemeSettings -> {
                            val currentSkin = settings?.skinTheme ?: SkinTheme.CLASSIC_WHITE

                            val items = listOf(
                                SettingsRowItem(
                                    id = "classic",
                                    title = "Classic White",
                                    subtitle = "Apple Polycarbonate & Silver Chamfer",
                                    valueLabel = if (currentSkin == SkinTheme.CLASSIC_WHITE) "ACTIVE" else "",
                                    onClick = {
                                        scope.launch { settingsRepo.setSkinTheme(SkinTheme.CLASSIC_WHITE) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "u2",
                                    title = "U2 Special Edition",
                                    subtitle = "Matte Black Body with Blood-Red Wheel",
                                    valueLabel = if (currentSkin == SkinTheme.U2_EDITION) "ACTIVE" else "",
                                    onClick = {
                                        scope.launch { settingsRepo.setSkinTheme(SkinTheme.U2_EDITION) }
                                    }
                                ),
                                SettingsRowItem(
                                    id = "aluminum",
                                    title = "Brushed Aluminum",
                                    subtitle = "Titanium Anodized Aluminum & Space Gray",
                                    valueLabel = if (currentSkin == SkinTheme.BRUSHED_ALUMINUM) "ACTIVE" else "",
                                    onClick = {
                                        scope.launch { settingsRepo.setSkinTheme(SkinTheme.BRUSHED_ALUMINUM) }
                                    }
                                )
                            )
                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSettingsListScreen(
                                title = "Skin Themes",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, items.size - 1),
                                listState = listState,
                                palette = screenPalette
                            )
                        }

                        is IpodScreen.StickerSettings -> {
                            val items = mutableListOf<SettingsRowItem>()

                            // Edit Mode Toggle
                            items.add(
                                SettingsRowItem(
                                    id = "toggle_edit_mode",
                                    title = if (isStickerEditMode) "Exit Edit Mode (Lock)" else "Enter Edit Mode (Unlock)",
                                    subtitle = if (isStickerEditMode) "Stickers are editable • Tap to lock in place" else "Unlock stickers to drag, scale & rotate",
                                    valueLabel = if (isStickerEditMode) "EDITING" else "LOCKED",
                                    onClick = {
                                        isStickerEditMode = !isStickerEditMode
                                        Toast.makeText(
                                            context,
                                            if (isStickerEditMode) "Sticker Edit Mode ON! Drag & 2-finger rotate on chassis" else "Stickers locked",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            )

                            if (stickers.size < 3) {
                                items.add(
                                    SettingsRowItem(
                                        id = "add_preset",
                                        title = "+ Add Preset Sticker",
                                        subtitle = "Rainbow Apple, Hi-Res, Advisory, Vinyl...",
                                        valueLabel = "${stickers.size}/3",
                                        onClick = { showPresetStickerDialog = true }
                                    )
                                )
                                items.add(
                                    SettingsRowItem(
                                        id = "add_custom",
                                        title = "+ Add from Gallery",
                                        subtitle = "Import your own custom photo / decal",
                                        onClick = { customStickerPickerLauncher.launch("image/*") }
                                    )
                                )
                            } else {
                                items.add(
                                    SettingsRowItem(
                                        id = "limit_reached",
                                        title = "Max 3 Stickers Active",
                                        subtitle = "Remove a sticker below to add another",
                                        valueLabel = "3/3 Max",
                                        onClick = { }
                                    )
                                )
                            }

                            // For each active sticker, provide resize, reset position, and remove options
                            val allPresets = remember(context) { StickerPresets.getAllPresets(context) }
                            stickers.forEachIndexed { index, st ->
                                val stickerName = when (st.type) {
                                    com.retro.ipod.data.sticker.StickerType.ASSET_IMAGE,
                                    com.retro.ipod.data.sticker.StickerType.PRESET_VECTOR -> {
                                        allPresets.firstOrNull { it.id == st.presetId }?.title
                                            ?: st.assetPath?.substringAfterLast('/')?.substringBeforeLast('.')
                                            ?: "Sticker #${index + 1}"
                                    }
                                    com.retro.ipod.data.sticker.StickerType.CUSTOM_GALLERY -> "Custom Photo #${index + 1}"
                                }

                                val scalePercent = (st.scale * 100).toInt()

                                items.add(
                                    SettingsRowItem(
                                        id = "sticker_${st.id}_resize",
                                        title = "$stickerName: Size",
                                        subtitle = "Tap to cycle: Small / Med / Large",
                                        valueLabel = "${scalePercent}%",
                                        onClick = {
                                            val nextScale = when {
                                                st.scale < 0.85f -> 1.0f
                                                st.scale < 1.2f -> 1.4f
                                                st.scale < 1.6f -> 1.8f
                                                else -> 0.7f
                                            }
                                            stickersRepo.updateSticker(st.copy(scale = nextScale))
                                            Toast.makeText(context, "Resized to ${(nextScale * 100).toInt()}%", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                )

                                val rotationDeg = ((st.rotation % 360f + 360f) % 360f).toInt()
                                items.add(
                                    SettingsRowItem(
                                        id = "sticker_${st.id}_rotate",
                                        title = "$stickerName: Rotate",
                                        subtitle = "Tap to rotate +45° • or 2 fingers on chassis",
                                        valueLabel = "${rotationDeg}°",
                                        onClick = {
                                            val newRot = (st.rotation + 45f) % 360f
                                            stickersRepo.updateSticker(st.copy(rotation = newRot))
                                            Toast.makeText(context, "Rotated to ${newRot.toInt()}°", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                )

                                items.add(
                                    SettingsRowItem(
                                        id = "sticker_${st.id}_reset_pos",
                                        title = "$stickerName: Reset Pos & Angle",
                                        subtitle = "Reset to open chassis space at 0°",
                                        onClick = {
                                            val newPos = StickersRepository.DEFAULT_INITIAL_POSITIONS.getOrElse(index) { 0.5f to 0.48f }
                                            stickersRepo.updateSticker(st.copy(normalizedX = newPos.first, normalizedY = newPos.second, rotation = 0f))
                                            Toast.makeText(context, "Position & angle reset", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                )

                                items.add(
                                    SettingsRowItem(
                                        id = "sticker_${st.id}_remove",
                                        title = "Remove $stickerName",
                                        subtitle = "Peel off this sticker",
                                        valueLabel = "✕",
                                        onClick = {
                                            stickersRepo.removeSticker(st.id)
                                            Toast.makeText(context, "Removed sticker", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                )
                            }

                            if (stickers.isNotEmpty()) {
                                items.add(
                                    SettingsRowItem(
                                        id = "clear_all_stickers",
                                        title = "Remove All Stickers",
                                        subtitle = "Peel off all decals from iPod",
                                        valueLabel = "Clear",
                                        onClick = {
                                            stickersRepo.clearAllStickers()
                                            Toast.makeText(context, "All stickers removed", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                )
                            }

                            activeItemCount = items.size
                            activeSelectAction = {
                                if (items.isNotEmpty() && activeHighlightedIndex in items.indices) {
                                    items[activeHighlightedIndex].onClick()
                                }
                            }

                            IpodSettingsListScreen(
                                title = "iPod Stickers",
                                items = items,
                                highlightedIndex = activeHighlightedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
                                listState = listState,
                                palette = screenPalette
                            )
                        }
                    }
                }
            }
        },
        wheelContent = {
            // Authentic Click Wheel with Haptics and Dynamic Skin Colorway
            ClickWheel(
                wheelSize = 220.dp,
                centerButtonSize = 78.dp,
                theme = activeSkin,
                hapticEnabled = settings?.hapticFeedbackEnabled ?: true,
                onRotateStep = { isClockwise ->
                    when (currentScreen) {
                        is IpodScreen.NowPlayingScreen -> {
                            val current = highPrecisionPositionMs
                            val delta = if (isClockwise) 2500L else -2500L
                            playerManager.seekTo((current + delta).coerceIn(0L, playbackState.durationMs))
                        }
                        is IpodScreen.LyricsScreen -> {
                            val current = highPrecisionPositionMs
                            val delta = if (isClockwise) 2500L else -2500L
                            playerManager.seekTo((current + delta).coerceIn(0L, playbackState.durationMs))
                        }
                        else -> {
                            if (activeItemCount > 0) {
                                activeHighlightedIndex = if (isClockwise) {
                                    (activeHighlightedIndex + 1).coerceAtMost(activeItemCount - 1)
                                } else {
                                    (activeHighlightedIndex - 1).coerceAtLeast(0)
                                }
                                scope.launch { listState.animateScrollToItem(activeHighlightedIndex) }
                            }
                        }
                    }
                },
                onCenterClick = {
                    when (currentScreen) {
                        is IpodScreen.NowPlayingScreen -> playerManager.togglePlayPause()
                        is IpodScreen.LyricsScreen -> playerManager.togglePlayPause()
                        else -> {
                            activeSelectAction?.invoke()
                        }
                    }
                },
                onMenuClick = {
                    val wasPopped = popBack()
                    if (!wasPopped) {
                        // At root MainMenu: jump to Now Playing if playing
                        if (playbackState.isPlaying || playbackState.currentSong != null) {
                            navigateTo(IpodScreen.NowPlayingScreen)
                        }
                    }
                },
                onPlayPauseClick = {
                    playerManager.togglePlayPause()
                },
                onNextClick = {
                    playerManager.skipToNext()
                },
                onPrevClick = {
                    playerManager.skipToPrevious()
                }
            )
        }
    )
}

@Composable
fun IpodMiniHeaderBar(
    dacState: UsbDacState,
    batteryState: DeviceBatteryState,
    currentSong: SongEntity?,
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: Int,
    palette: com.retro.ipod.ui.theme.IpodScreenPalette,
    onDacBadgeClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        color = palette.headerBarBackground,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Play state + Shuffle/Repeat indicators + Route Pill (DAC Name)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = if (isPlaying) "Playing" else "Paused",
                    tint = palette.headerBarText,
                    modifier = Modifier
                        .size(11.dp)
                        .padding(end = 4.dp)
                )

                if (shuffleMode) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = palette.headerBarText,
                        modifier = Modifier
                            .size(10.dp)
                            .padding(end = 4.dp)
                    )
                }

                if (repeatMode == Player.REPEAT_MODE_ALL) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = "Repeat All",
                        tint = palette.headerBarText,
                        modifier = Modifier
                            .size(10.dp)
                            .padding(end = 4.dp)
                    )
                } else if (repeatMode == Player.REPEAT_MODE_ONE) {
                    Icon(
                        imageVector = Icons.Rounded.RepeatOne,
                        contentDescription = "Repeat One",
                        tint = palette.headerBarText,
                        modifier = Modifier
                            .size(10.dp)
                            .padding(end = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (dacState.isClaimed) Color(0xFF332D15) else palette.badgeBackground,
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { onDacBadgeClick() }
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = dacState.statusBadgeText,
                        color = if (dacState.isClaimed) Color(0xFFFFD54F) else palette.headerBarMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFonts.helvetica(FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Center: Format & Stream Metrics
            val codecText = currentSong?.let { "${it.codec} ${it.sampleRateKhz}kHz / ${it.bitDepth}bit" } ?: "FLAC 44.1kHz / 24bit"
            Text(
                text = codecText,
                color = palette.headerBarText,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFonts.helvetica(FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Right: Real Device Battery State (iPod Skeuomorphic Shell + Nub + Dynamic % + Charging Bolt)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 8.dp)
                            .border(1.dp, palette.batteryBorder, RoundedCornerShape(1.5.dp))
                            .padding(1.dp)
                    ) {
                        val fillPct = (batteryState.level / 100f).coerceIn(0.06f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = fillPct)
                                .background(
                                    if (batteryState.level <= 15 && !batteryState.isCharging) Color(0xFFE53935)
                                    else palette.batteryFill,
                                    RoundedCornerShape(0.5.dp)
                                )
                        )
                    }
                    // Authentic iPod positive battery terminal nub
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(3.5.dp)
                            .background(
                                palette.batteryBorder,
                                RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp)
                            )
                    )
                }

                if (batteryState.isCharging) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = "Charging",
                        tint = palette.batteryBorder,
                        modifier = Modifier.size(9.dp)
                    )
                }

                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${batteryState.level}%",
                    color = palette.batteryBorder,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFonts.helvetica(FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun DacHardwareDialog(
    dacState: UsbDacState,
    onDismiss: () -> Unit,
    onToggleExclusive: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E26),
        title = {
            Text(
                text = "USB DAC HARDWARE DETAILS",
                color = Color(0xFFFFD54F),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Route", if (dacState.isClaimed) "Bit-Perfect USB DAC" else "Android Direct HAL")
                DetailRow("Device Name", if (dacState.deviceName.isNotBlank() && !dacState.deviceName.equals("DIRECT", ignoreCase = true) && !dacState.deviceName.equals("Internal DAC", ignoreCase = true)) dacState.deviceName else "DIRECT (Internal Audio)")
                if (dacState.vendorId != 0) {
                    DetailRow("USB Vendor/Product", "VID: 0x${dacState.vendorId.toString(16).uppercase()} | PID: 0x${dacState.productId.toString(16).uppercase()}")
                }
                DetailRow("Streaming Interface", if (dacState.hasAudioStreamingInterface) "UAC 2.0 Detected" else "Standard AudioTrack")
                DetailRow("Oboe C++ Pipeline", if (NativeAudioBridge.isLoaded()) "Oboe ${NativeAudioBridge.getOboeVersion()}" else "Direct High-Res HAL")

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bit-Perfect Exclusive",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Bypass Android AudioFlinger mixer",
                            color = Color(0xFF9E9E9E),
                            fontSize = 9.sp
                        )
                    }

                    Switch(
                        checked = dacState.isExclusiveModeEnabled,
                        onCheckedChange = { onToggleExclusive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFD54F),
                            checkedTrackColor = Color(0xFF332D15)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = Color(0xFF4FC3F7))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF8E8E93), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
