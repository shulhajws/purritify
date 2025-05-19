package com.example.purrytify.ui.playback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.DialogFragment
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.AudioDevice
import com.example.purrytify.util.AudioRouteManager

class AudioOutputDialog(
    private val audioRouteManager: AudioRouteManager,
    private val onDismiss: () -> Unit
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                PurrytifyTheme {
                    AudioOutputDialogContent(
                        audioRouteManager = audioRouteManager,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}

@Composable
fun AudioOutputDialogContent(
    audioRouteManager: AudioRouteManager,
    onDismiss: () -> Unit
) {
    val devices by audioRouteManager.audioDevices.collectAsState()
    val currentDevice by audioRouteManager.currentDevice.collectAsState()
    val errorMessage by audioRouteManager.errorMessage.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            audioRouteManager.updateDeviceList()
            isRefreshing = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Audio Output",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row {
                        IconButton(onClick = { isRefreshing = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Error Message
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Devices List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(devices) { device ->
                        AudioDeviceItem(
                            device = device,
                            isSelected = device.id == currentDevice?.id,
                            onClick = {
                                audioRouteManager.selectDevice(device)
                            }
                        )
                    }

                    if (devices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No audio devices found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioDeviceItem(
    device: AudioDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val deviceIcon = when (device.deviceType) {
        AudioDevice.DeviceType.INTERNAL_SPEAKER -> Icons.Filled.Speaker
        AudioDevice.DeviceType.WIRED_HEADSET -> Icons.Filled.Headphones
        AudioDevice.DeviceType.BLUETOOTH -> Icons.Filled.Bluetooth
        AudioDevice.DeviceType.USB_AUDIO -> Icons.Filled.Usb
        else -> Icons.Filled.Speaker
    }

    val isConnected = device.isConnected
    val connectionStatus = if (isConnected) "Connected" else "Disconnected"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isConnected) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = deviceIcon,
            contentDescription = null,
            tint = if (isConnected)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (!isConnected)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = connectionStatus,
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected)
                    MaterialTheme.colorScheme.onSurface
                else
                    Color.Gray
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}