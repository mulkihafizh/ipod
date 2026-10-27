package com.retro.ipod

import android.app.Application
import com.retro.ipod.data.db.IpodDatabase
import com.retro.ipod.data.scanner.AudioMetadataScanner
import com.retro.ipod.data.settings.AppSettingsRepository

/**
 * Base Application class initializing global singletons for Room Database,
 * Settings Repository, and Media Scanner.
 */
class IpodApplication : Application() {

    lateinit var database: IpodDatabase
        private set

    lateinit var settingsRepository: AppSettingsRepository
        private set

    lateinit var metadataScanner: AudioMetadataScanner
        private set

    lateinit var playerManager: com.retro.ipod.playback.MusicPlayerManager
        private set

    lateinit var usbAudioDeviceManager: com.retro.ipod.playback.audio.UsbAudioDeviceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = IpodDatabase.getInstance(this)
        settingsRepository = AppSettingsRepository(this)
        metadataScanner = AudioMetadataScanner(this)
        playerManager = com.retro.ipod.playback.MusicPlayerManager(this)
        usbAudioDeviceManager = com.retro.ipod.playback.audio.UsbAudioDeviceManager(this, settingsRepository)
    }

    companion object {
        lateinit var instance: IpodApplication
            private set
    }
}
