package com.retro.ipod.playback.audio

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.retro.ipod.data.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages USB Host connection, detection of audiophile USB DACs (UAC1/UAC2),
 * and claims USB Audio Streaming interfaces for Bit-Perfect audio routing.
 */
class UsbAudioDeviceManager(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository
) {
    companion object {
        private const val TAG = "UsbAudioDeviceManager"
        const val ACTION_USB_PERMISSION = "com.retro.ipod.USB_PERMISSION"
        private const val USB_SUBCLASS_AUDIO_STREAMING = 2
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _dacState = MutableStateFlow(UsbDacState())
    val dacState: StateFlow<UsbDacState> = _dacState.asStateFlow()

    private var activeConnection: UsbDeviceConnection? = null
    private var claimedInterface: UsbInterface? = null
    private var activeDevice: UsbDevice? = null

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added - refreshing audio routing")
            updateAudioRoutingInfo()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed - refreshing audio routing")
            updateAudioRoutingInfo()
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { handleDeviceAttached(it) }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { handleDeviceDetached(it) }
                }

                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (device != null && granted) {
                            Log.i(TAG, "USB Permission granted for device: ${device.productName}")
                            claimUsbDac(device)
                        } else {
                            Log.w(TAG, "USB Permission denied for device: ${device?.productName}")
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }

        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register audioDeviceCallback", e)
        }

        // Listen for setting changes
        scope.launch {
            settingsRepository.appSettingsFlow.collect { settings ->
                _dacState.update { it.copy(isExclusiveModeEnabled = settings.usbExclusiveMode) }
                if (settings.usbExclusiveMode && activeDevice != null && !dacState.value.isClaimed) {
                    claimUsbDac(activeDevice!!)
                } else if (!settings.usbExclusiveMode && dacState.value.isClaimed) {
                    releaseClaimedInterface()
                }
            }
        }

        // Check already connected USB & Audio devices on startup
        updateAudioRoutingInfo()
        scanConnectedUsbDevices()
    }

    /**
     * Resolves the actual DAC or active audio output hardware name from Android framework.
     */
    fun updateAudioRoutingInfo() {
        try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val usbDev = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
            }

            if (usbDev != null) {
                val pName = usbDev.productName?.toString()?.trim()
                val dacName = if (!pName.isNullOrBlank()) pName else "USB Audio DAC"
                _dacState.update { current ->
                    current.copy(
                        isConnected = true,
                        deviceName = dacName,
                        routeName = if (current.isClaimed) "USB-DAC" else dacName
                    )
                }
                Log.i(TAG, "Audio routing identified USB DAC: $dacName")
                return
            }

            // If a USB device is attached via UsbManager
            val currentAttached = activeDevice
            if (currentAttached != null) {
                val prodName = currentAttached.productName?.trim()
                val dacName = if (!prodName.isNullOrBlank()) prodName else "USB Audio DAC"
                _dacState.update { current ->
                    current.copy(
                        isConnected = true,
                        deviceName = dacName
                    )
                }
                return
            }

            // Wired Headphones
            val wiredDev = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            }
            if (wiredDev != null) {
                val name = wiredDev.productName?.toString()?.trim() ?: "Headphones"
                _dacState.update { current ->
                    current.copy(
                        isConnected = false,
                        deviceName = name
                    )
                }
                return
            }

            // Bluetooth Audio
            val btDev = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            if (btDev != null) {
                val name = btDev.productName?.toString()?.trim() ?: "Bluetooth Audio"
                _dacState.update { current ->
                    current.copy(
                        isConnected = false,
                        deviceName = name
                    )
                }
                return
            }

            // Default internal audio
            _dacState.update { current ->
                if (current.isConnected && current.deviceName.isNotBlank() && current.deviceName != "DIRECT" && current.deviceName != "Internal DAC") {
                    current
                } else {
                    current.copy(
                        isConnected = false,
                        isClaimed = false,
                        deviceName = "DIRECT"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating audio routing info", e)
        }
    }

    /**
     * Scans for already attached USB devices to locate any USB Audio Class device.
     */
    fun scanConnectedUsbDevices() {
        val devices = usbManager.deviceList
        for (device in devices.values) {
            if (isAudioDevice(device)) {
                handleDeviceAttached(device)
                return
            }
        }
        updateAudioRoutingInfo()
    }

    private fun isAudioDevice(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
        }
        return false
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        if (!isAudioDevice(device)) return
        activeDevice = device

        val hasStreaming = (0 until device.interfaceCount).any { i ->
            val iface = device.getInterface(i)
            iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO &&
                    iface.interfaceSubclass == USB_SUBCLASS_AUDIO_STREAMING
        }

        // Try getting name from UsbDevice, or fallback to AudioManager device name
        val audioMgrName = try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
            }?.productName?.toString()?.trim()
        } catch (_: Exception) { null }

        val name = when {
            !device.productName.isNullOrBlank() && device.productName != "USB Audio DAC" -> device.productName!!
            !audioMgrName.isNullOrBlank() -> audioMgrName
            !device.productName.isNullOrBlank() -> device.productName!!
            else -> "USB Audio DAC"
        }
        val manufacturer = device.manufacturerName ?: ""

        Log.i(TAG, "USB Audio DAC attached: $name (VID: ${device.vendorId}, PID: ${device.productId})")

        _dacState.update {
            it.copy(
                isConnected = true,
                deviceName = name,
                manufacturerName = manufacturer,
                vendorId = device.vendorId,
                productId = device.productId,
                interfaceCount = device.interfaceCount,
                hasAudioStreamingInterface = hasStreaming,
                routeName = if (it.isClaimed) "USB-DAC" else name
            )
        }

        if (dacState.value.isExclusiveModeEnabled) {
            requestPermissionAndClaim(device)
        }
    }

    private fun handleDeviceDetached(device: UsbDevice) {
        if (activeDevice?.deviceId == device.deviceId) {
            Log.i(TAG, "USB Audio DAC detached: ${device.productName}")
            releaseClaimedInterface()
            activeDevice = null

            _dacState.update {
                it.copy(
                    isConnected = false,
                    isClaimed = false,
                    deviceName = "DIRECT",
                    manufacturerName = "",
                    vendorId = 0,
                    productId = 0,
                    routeName = "DIRECT"
                )
            }
            updateAudioRoutingInfo()
        }
    }

    fun requestPermissionAndClaim(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            claimUsbDac(device)
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                flags
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    fun claimUsbDac(device: UsbDevice) {
        try {
            val connection = usbManager.openDevice(device) ?: run {
                Log.e(TAG, "Failed to open USB device: ${device.productName}")
                return
            }

            var streamingIface: UsbInterface? = null
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO &&
                    iface.interfaceSubclass == USB_SUBCLASS_AUDIO_STREAMING
                ) {
                    streamingIface = iface
                    break
                }
            }

            if (streamingIface != null) {
                val claimed = connection.claimInterface(streamingIface, true)
                if (claimed) {
                    activeConnection = connection
                    claimedInterface = streamingIface
                    Log.i(TAG, "Successfully claimed UAC2 audio streaming interface on ${device.productName}")

                    // Initialize Oboe native stream if library is available
                    if (NativeAudioBridge.isLoaded()) {
                        NativeAudioBridge.initAudioStream(sampleRate = 44100, channelCount = 2, bitDepth = 24)
                    }

                    _dacState.update {
                        it.copy(
                            isClaimed = true,
                            routeName = "USB-DAC"
                        )
                    }
                } else {
                    Log.w(TAG, "Failed to claim USB interface on ${device.productName}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error claiming USB DAC interface", e)
        }
    }

    private fun releaseClaimedInterface() {
        try {
            if (NativeAudioBridge.isLoaded()) {
                NativeAudioBridge.closeAudioStream()
            }
            claimedInterface?.let { activeConnection?.releaseInterface(it) }
            activeConnection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing USB interface", e)
        } finally {
            claimedInterface = null
            activeConnection = null
            _dacState.update { it.copy(isClaimed = false, routeName = "DIRECT") }
        }
    }

    fun toggleExclusiveMode(enabled: Boolean) {
        scope.launch {
            settingsRepository.setUsbExclusiveMode(enabled)
        }
    }

    fun release() {
        releaseClaimedInterface()
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            // Callback might not be registered
        }
    }
}
