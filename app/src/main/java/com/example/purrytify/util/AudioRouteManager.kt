package com.example.purrytify.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioDevice(
    val id: String,
    val name: String,
    val isConnected: Boolean = false,
    val isActive: Boolean = false,
    val deviceType: DeviceType,
    val deviceInfo: AudioDeviceInfo? = null,
    val bluetoothDevice: BluetoothDevice? = null
) {
    enum class DeviceType {
        INTERNAL_SPEAKER,
        WIRED_HEADSET,
        BLUETOOTH,
        USB_AUDIO,
        OTHER
    }

    override fun toString(): String {
        return "AudioDevice(id=$id, name=$name, type=$deviceType, connected=$isConnected, active=$isActive)"
    }
}

class AudioRouteManager(private val context: Context) {
    private val tag = "AudioRouteManager"

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter

    private val _audioDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val audioDevices: StateFlow<List<AudioDevice>> = _audioDevices.asStateFlow()

    private val _currentDevice = MutableStateFlow<AudioDevice?>(null)
    val currentDevice: StateFlow<AudioDevice?> = _currentDevice.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var a2dpProxy: BluetoothProfile? = null

    private val deviceNames = mutableSetOf<String>()
    private val processedBluetoothAddresses = mutableSetOf<String>()
    private val phoneBuiltInSpeakerNames = setOf("built-in speaker", "phone", "speaker", "speakers", "internal")

    init {
        refreshDeviceList()
        registerAudioDeviceCallback()
        registerBluetoothReceiver()
    }

    private fun refreshDeviceList() {
        val devices = mutableListOf<AudioDevice>()
        deviceNames.clear()
        processedBluetoothAddresses.clear()

        val isBluetoothActive = isBluetoothAudioActive()

        val internalSpeaker = AudioDevice(
            id = "internal_speaker",
            name = "Device Speaker",
            isConnected = true,
            isActive = !isBluetoothActive,
            deviceType = AudioDevice.DeviceType.INTERNAL_SPEAKER
        )
        devices.add(internalSpeaker)
        deviceNames.add("Device Speaker")

        if (hasBluetoothPermission()) {
            collectBluetoothDevices(devices, isBluetoothActive)
        }
        collectAudioDevices(devices)

        val filteredDevices = devices.distinctBy { it.id }
        _audioDevices.value = filteredDevices

        val activeDevice = determineCurrentActiveDevice(devices)
        if (activeDevice != null) {
            _currentDevice.value = activeDevice
            _audioDevices.value = devices.map {
                it.copy(isActive = it.id == activeDevice.id)
            }
        } else if (_currentDevice.value == null) {
            _currentDevice.value = internalSpeaker
        }

        Log.d(tag, "Device list refreshed. Found ${devices.size} unique audio devices")
    }

    private fun determineCurrentActiveDevice(devices: List<AudioDevice>): AudioDevice? {
        if (isBluetoothAudioActive()) {
            return devices.firstOrNull { it.deviceType == AudioDevice.DeviceType.BLUETOOTH }
        }

        if (audioManager.isWiredHeadsetOn) {
            return devices.firstOrNull { it.deviceType == AudioDevice.DeviceType.WIRED_HEADSET }
        }

        return devices.firstOrNull { it.deviceType == AudioDevice.DeviceType.INTERNAL_SPEAKER }
    }

    private fun isBluetoothAudioActive(): Boolean {
        val isBluetoothScoOn = audioManager.isBluetoothScoOn

        val isA2dpConnected = if (hasBluetoothPermission() && bluetoothAdapter != null) {
            try {
                val connectedDevices = mutableListOf<BluetoothDevice>()
                val latch = java.util.concurrent.CountDownLatch(1)

                bluetoothAdapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (hasBluetoothPermission()) {
                            connectedDevices.addAll(proxy.connectedDevices)
                        }
                        latch.countDown()
                    }

                    override fun onServiceDisconnected(profile: Int) {
                    }
                }, BluetoothProfile.A2DP)

                latch.await(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                connectedDevices.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }

        return isBluetoothScoOn || isA2dpConnected
    }

    private fun collectBluetoothDevices(devices: MutableList<AudioDevice>, isBluetoothActive: Boolean) {
        try {
            val connectedDevices = mutableListOf<BluetoothDevice>()
            val latch = java.util.concurrent.CountDownLatch(1)

            bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    a2dpProxy = proxy
                    if (hasBluetoothPermission()) {
                        connectedDevices.addAll(proxy.connectedDevices)
                    }
                    latch.countDown()
                }

                override fun onServiceDisconnected(profile: Int) {
                    a2dpProxy = null
                }
            }, BluetoothProfile.A2DP)

            latch.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)

            for (device in connectedDevices) {
                if (!hasBluetoothPermission()) continue

                val address = device.address
                if (processedBluetoothAddresses.contains(address)) continue

                val name = getBluetoothDeviceName(device)

                devices.add(AudioDevice(
                    id = address,
                    name = name,
                    isConnected = true,
                    isActive = isBluetoothActive,
                    deviceType = AudioDevice.DeviceType.BLUETOOTH,
                    bluetoothDevice = device
                ))

                deviceNames.add(name)
                processedBluetoothAddresses.add(address)
            }

        } catch (e: Exception) {
            Log.e(tag, "Error collecting Bluetooth devices: ${e.message}")
        }
    }

    private fun getBluetoothDeviceName(device: BluetoothDevice): String {
        return if (hasBluetoothPermission()) {
            try {
                device.name ?: "Bluetooth Device"
            } catch (e: SecurityException) {
                "Bluetooth Device"
            }
        } else {
            "Bluetooth Device"
        }
    }

    private fun collectAudioDevices(devices: MutableList<AudioDevice>) {
        try {
            val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in audioDevices) {
                if (isInternalSpeaker(device)) {
                    continue
                }

                val deviceType = when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        if (processedBluetoothAddresses.contains(device.address)) {
                            continue
                        }
                        AudioDevice.DeviceType.BLUETOOTH
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioDevice.DeviceType.WIRED_HEADSET
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDevice.DeviceType.USB_AUDIO
                    else -> AudioDevice.DeviceType.OTHER
                }

                val baseName = device.productName?.toString() ?: "Audio Device"

                if (deviceNames.contains(baseName)) {
                    continue
                }

                var deviceName = baseName
                var counter = 1

                while (deviceNames.contains(deviceName)) {
                    deviceName = "$baseName ($counter)"
                    counter++
                }

                val deviceId = run {
                    processedBluetoothAddresses.add(device.address)
                    "bt_${device.address}"
                }

                devices.add(AudioDevice(
                    id = deviceId,
                    name = deviceName,
                    isConnected = true,
                    isActive = false,
                    deviceType = deviceType,
                    deviceInfo = device
                ))

                deviceNames.add(deviceName)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error collecting audio devices: ${e.message}")
        }
    }

    private fun isInternalSpeaker(device: AudioDeviceInfo): Boolean {
        if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) return true

        val productName = device.productName?.toString()?.lowercase() ?: ""
        if (phoneBuiltInSpeakerNames.any { it in productName }) return true

        val modelName = Build.MODEL.lowercase()
        if (productName == modelName) return true

        return false
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun registerAudioDeviceCallback() {
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                Log.d(tag, "Audio devices added: ${addedDevices.size}")
                refreshDeviceList()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
                Log.d(tag, "Audio devices removed: ${removedDevices.size}")

                val currentDeviceId = _currentDevice.value?.id
                var currentDeviceRemoved = false

                for (device in removedDevices) {
                    val deviceId = "bt_${device.address}"
                    if (deviceId == currentDeviceId) {
                        currentDeviceRemoved = true
                        break
                    }
                }

                refreshDeviceList()

                if (currentDeviceRemoved) {
                    _errorMessage.value = "Audio device disconnected. Switching to internal speaker."
                    switchToInternalSpeaker()
                }
            }
        }

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    private fun registerBluetoothReceiver() {
        if (bluetoothAdapter != null) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }

            bluetoothReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }

                            device?.let {
                                if (hasBluetoothPermission()) {
                                    Log.d(tag, "Bluetooth device connected: ${getBluetoothDeviceName(device)}")
                                }
                                refreshDeviceList()
                            }
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }

                            device?.let {
                                if (hasBluetoothPermission()) {
                                    Log.d(tag, "Bluetooth device disconnected: ${getBluetoothDeviceName(device)}")

                                    val deviceId = "bt_${device.address}"
                                    if (_currentDevice.value?.id == deviceId) {
                                        _errorMessage.value = "Bluetooth device disconnected. Switching to internal speaker."
                                        switchToInternalSpeaker()
                                    }
                                }

                                refreshDeviceList()
                            }
                        }
                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                            if (state == BluetoothAdapter.STATE_OFF) {
                                Log.d(tag, "Bluetooth turned off")

                                if (_currentDevice.value?.deviceType == AudioDevice.DeviceType.BLUETOOTH) {
                                    _errorMessage.value = "Bluetooth turned off. Switching to internal speaker."
                                    switchToInternalSpeaker()
                                }

                                refreshDeviceList()
                            } else if (state == BluetoothAdapter.STATE_ON) {
                                Log.d(tag, "Bluetooth turned on")
                                refreshDeviceList()
                            }
                        }
                    }
                }
            }

            context.registerReceiver(bluetoothReceiver, filter)
        }
    }

    fun updateDeviceList() {
        refreshDeviceList()
    }

    fun selectDevice(device: AudioDevice): Boolean {
        return try {
            Log.d(tag, "Selecting audio device: ${device.name}")

            val updatedDevices = _audioDevices.value.map {
                it.copy(isActive = it.id == device.id)
            }
            _audioDevices.value = updatedDevices
            _currentDevice.value = device

            when (device.deviceType) {
                AudioDevice.DeviceType.INTERNAL_SPEAKER -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                    audioManager.mode = AudioManager.MODE_NORMAL

                    Log.d(tag, "Routed audio to internal speaker")
                }

                AudioDevice.DeviceType.BLUETOOTH -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    Log.d(tag, "Bluetooth device selected. Let system manage A2DP routing.")
                }

                AudioDevice.DeviceType.WIRED_HEADSET -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_NORMAL

                    Log.d(tag, "Routed audio to wired headset")
                }

                else -> {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_NORMAL

                    Log.d(tag, "Routed audio to other device")
                }
            }

            _errorMessage.value = null
            true
        } catch (e: Exception) {
            Log.e(tag, "Error selecting audio device: ${e.message}")
            _errorMessage.value = "Failed to select audio device: ${e.message}"
            false
        }
    }


    private fun switchToInternalSpeaker() {
        val internalSpeaker = _audioDevices.value.firstOrNull {
            it.deviceType == AudioDevice.DeviceType.INTERNAL_SPEAKER
        }

        if (internalSpeaker != null) {
            selectDevice(internalSpeaker)
        } else {
            Log.e(tag, "Internal speaker not found in device list")
        }
    }

    fun cleanup() {
        audioDeviceCallback?.let {
            audioManager.unregisterAudioDeviceCallback(it)
        }

        bluetoothReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(tag, "Error unregistering Bluetooth receiver: ${e.message}")
            }
        }

        if (bluetoothAdapter != null && a2dpProxy != null) {
            try {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProxy)
            } catch (e: Exception) {
                Log.e(tag, "Error closing A2DP proxy: ${e.message}")
            }
        }

        try {
            audioManager.isBluetoothScoOn = false
        } catch (e: Exception) {
            Log.e(tag, "Error stopping Bluetooth SCO: ${e.message}")
        }
    }
}