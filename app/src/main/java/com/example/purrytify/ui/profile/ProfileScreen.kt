package com.example.purrytify.ui.profile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.UserProfile
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.theme.DarkBlack
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.ui.theme.Turquoise
import com.example.purrytify.ui.theme.White
import com.example.purrytify.util.TokenManager

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    songsCount: Int = 0,
    likedCount: Int = 0,
    listenedCount: Int = 0,
    soundCapsuleViewModel: SoundCapsuleViewModel? = null,
    onSoundCapsuleNavigation: (String, MonthYear) -> Unit = { _, _ -> },
    onProfileUpdated: (UserProfile) -> Unit = {}
) {
    val context = LocalContext.current
    val editProfileViewModel: EditProfileViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    val editState by editProfileViewModel.state.collectAsState()

    // Dialog States
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    // Places Picker Launcher
    val placesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        editProfileViewModel.handlePlacePickerResult(
            resultCode = result.resultCode,
            data = result.data,
            onLocationSelected = { locationCode ->
                editProfileViewModel.updateProfile(
                    context = context,
                    location = locationCode,
                    onSuccess = { updatedProfile ->
                        onProfileUpdated(updatedProfile)
                        showLocationDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            editProfileViewModel.updateProfile(
                context = context,
                imageUri = it,
                onSuccess = { updatedProfile ->
                    onProfileUpdated(updatedProfile)
                },
                onError = { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            editState.selectedImageUri?.let { uri ->
                editProfileViewModel.updateProfile(
                    context = context,
                    imageUri = uri,
                    onSuccess = { updatedProfile ->
                        onProfileUpdated(updatedProfile)
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Permission Launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            editProfileViewModel.getCurrentLocation(context) { locationCode ->
                editProfileViewModel.updateProfile(
                    context = context,
                    location = locationCode,
                    onSuccess = { updatedProfile ->
                        onProfileUpdated(updatedProfile)
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cameraIntent = editProfileViewModel.createImageCaptureIntent(context)
            cameraIntent?.let { intent ->
                val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                }
                photoUri?.let {
                    editProfileViewModel.setSelectedImageUri(it)
                    cameraLauncher.launch(it)
                }
            }
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Turquoise, DarkBlack),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        item {
            // Profile Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Profile Picture with Edit functionality
                Box(
                    modifier = Modifier.size(140.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = userProfile.profilePhoto),
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .clickable { showImagePickerDialog = true }
                    )

                    // Edit Icon Overlay
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen)
                            .clickable { showImagePickerDialog = true }
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit profile picture",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Loading Indicator
                    if (editState.isUpdatingProfile) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = SpotifyGreen,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Information
                Text(
                    text = userProfile.username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = White
                )

                // Location with edit functionality
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showLocationDialog = true }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { showLocationDialog = true }
                            )
                        }
                        .padding(4.dp)
                ) {
                    Text(
                        text = editProfileViewModel.getCountryNameFromCode(userProfile.location),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftGray
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit location",
                        tint = SoftGray,
                        modifier = Modifier.size(16.dp)
                    )

                    if (editState.isLocationLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                Button(
                    onClick = {
                        TokenManager.clearToken(context)
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Logout",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        count = songsCount,
                        label = "SONGS"
                    )

                    StatItem(
                        count = likedCount,
                        label = "LIKED"
                    )

                    StatItem(
                        count = listenedCount,
                        label = "LISTENED"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Sound Capsule Section
        item {
            soundCapsuleViewModel?.let { viewModel ->
                OverviewSoundCapsule(
                    viewModel = viewModel,
                    onTimeListenedClick = { monthYear ->
                        onSoundCapsuleNavigation("time_listened", monthYear)
                    },
                    onTopArtistsClick = { monthYear ->
                        onSoundCapsuleNavigation("top_artists", monthYear)
                    },
                    onTopSongsClick = { monthYear ->
                        onSoundCapsuleNavigation("top_songs", monthYear)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Image Picker Dialog
    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = {
                Text("Change Profile Picture")
            },
            text = {
                Text("Choose how you want to update your profile picture:")
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            val cameraPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.CAMERA
                            } else {
                                Manifest.permission.CAMERA
                            }

                            if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                                val cameraIntent = editProfileViewModel.createImageCaptureIntent(context)
                                cameraIntent?.let { intent ->
                                    val photoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
                                    }

                                    photoUri?.let {
                                        editProfileViewModel.setSelectedImageUri(it)
                                        cameraLauncher.launch(it)
                                    }
                                }
                            } else {
                                cameraPermissionLauncher.launch(cameraPermission)
                            }
                        }
                    ) {
                        Text("Camera")
                    }

                    TextButton(
                        onClick = {
                            showImagePickerDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Gallery")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImagePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Location Dialog
    if (showLocationDialog) {
        LocationSelectionDialog(
            editProfileViewModel = editProfileViewModel,
            onDismiss = { showLocationDialog = false },
            onLocationSelected = { locationCode ->
                editProfileViewModel.updateProfile(
                    context = context,
                    location = locationCode,
                    onSuccess = { updatedProfile ->
                        onProfileUpdated(updatedProfile)
                        showLocationDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onCurrentLocationRequested = {
                val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
                val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION

                if (ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED) {
                    editProfileViewModel.getCurrentLocation(context) { locationCode ->
                        editProfileViewModel.updateProfile(
                            context = context,
                            location = locationCode,
                            onSuccess = { updatedProfile ->
                                onProfileUpdated(updatedProfile)
                                showLocationDialog = false
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } else {
                    locationPermissionLauncher.launch(arrayOf(fineLocationPermission, coarseLocationPermission))
                }
            },
            onCancelLocationRequest = {
                editProfileViewModel.cancelLocationRequest()
            },
            onPlacesPickerRequested = {
                try {
                    val intent = editProfileViewModel.createPlacesPickerIntent()
                    placesPickerLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open location picker: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionDialog(
    editProfileViewModel: EditProfileViewModel,
    onDismiss: () -> Unit,
    onLocationSelected: (String) -> Unit,
    onCurrentLocationRequested: () -> Unit,
    onCancelLocationRequest: () -> Unit,
    onPlacesPickerRequested: () -> Unit
) {
    val context = LocalContext.current
    val editState by editProfileViewModel.state.collectAsState()
    val availableCountries = editProfileViewModel.getAvailableCountries()

    var expanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(availableCountries.first()) }

    AlertDialog(
        onDismissRequest = {
            if (editState.isLocationLoading) {
                onCancelLocationRequest()
            }
            onDismiss()
        },
        title = { Text("Change Location") },
        text = {
            Column {
                Text("Select your location:")
                Spacer(modifier = Modifier.height(16.dp))

                // Auto Location Button
                Button(
                    onClick = if (editState.isLocationLoading) onCancelLocationRequest else onCurrentLocationRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editState.isLocationLoading) Color.Red else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (editState.isLocationLoading) {
                        Text("Cancel Location")
                    } else {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Current Location")
                    }
                }

                if (editState.isLocationLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SpotifyGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Getting location...")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Or select manually:")
                Spacer(modifier = Modifier.height(8.dp))

                // Country Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded && !editState.isLocationLoading }
                ) {
                    OutlinedTextField(
                        value = selectedCountry.first,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !editState.isLocationLoading,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableCountries.forEach { (countryName, countryCode) ->
                            DropdownMenuItem(
                                text = { Text(countryName) },
                                onClick = {
                                    selectedCountry = countryName to countryCode
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Places Picker Button
                Button(
                    onClick = onPlacesPickerRequested,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Location from Map")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 Tip: Use 'Pick Location from Map' for the best experience!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onLocationSelected(selectedCountry.second) },
                enabled = !editState.isLocationLoading
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (editState.isLocationLoading) onCancelLocationRequest()
                    onDismiss()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
    }
}