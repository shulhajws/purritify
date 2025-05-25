package com.example.purrytify.ui.profile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.UserProfile
import com.example.purrytify.services.RetrofitClient
import com.example.purrytify.util.TokenManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class EditProfileState(
    val isLoading: Boolean = false,
    val isLocationLoading: Boolean = false,
    val error: String? = null,
    val selectedImageUri: Uri? = null,
    val currentLocation: String? = null,
    val isUpdatingProfile: Boolean = false
)

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val countryCodes = mapOf(
        "Indonesia" to "ID",
        "Malaysia" to "MY",
        "United States" to "US",
        "United Kingdom" to "GB",
        "Switzerland" to "CH",
        "Germany" to "DE",
        "Brazil" to "BR"
    )

    private var locationTask: com.google.android.gms.tasks.Task<Location>? = null

    fun getCurrentLocation(context: Context, onLocationReceived: (String) -> Unit) {
        _state.value = _state.value.copy(isLocationLoading = true)

        try {
            locationTask = fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        getAddressFromLocation(context, location.latitude, location.longitude, onLocationReceived)
                    } else {
                        requestFreshLocation(context, onLocationReceived)
                    }
                }
                .addOnFailureListener { exception ->
                    _state.value = _state.value.copy(
                        isLocationLoading = false,
                        error = "Failed to get location: ${exception.message}"
                    )
                    Toast.makeText(context, "Failed to get location", Toast.LENGTH_SHORT).show()
                    Log.e("EditProfileViewModel", "Location error", exception)
                }
        } catch (e: SecurityException) {
            _state.value = _state.value.copy(
                isLocationLoading = false,
                error = "Location permission not granted"
            )
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestFreshLocation(context: Context, onLocationReceived: (String) -> Unit) {
        try {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                10000
            ).setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build()

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        getAddressFromLocation(context, location.latitude, location.longitude, onLocationReceived)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                fusedLocationClient.removeLocationUpdates(locationCallback)
                if (_state.value.isLocationLoading) {
                    _state.value = _state.value.copy(
                        isLocationLoading = false,
                        error = "Location request timed out"
                    )
                    Toast.makeText(context, "Location request timed out", Toast.LENGTH_SHORT).show()
                }
            }, 10000)

        } catch (e: SecurityException) {
            _state.value = _state.value.copy(
                isLocationLoading = false,
                error = "Location permission not granted"
            )
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelLocationRequest() {
        locationTask?.let {
            if (!it.isComplete) {
                it.isCanceled
            }
        }
        _state.value = _state.value.copy(isLocationLoading = false)
    }

    private fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double, onLocationReceived: (String) -> Unit) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val countryName = address.countryName
                        val countryCode = countryCodes[countryName] ?: "ID"

                        _state.value = _state.value.copy(
                            isLocationLoading = false,
                            currentLocation = countryCode
                        )
                        onLocationReceived(countryCode)
                    } else {
                        _state.value = _state.value.copy(
                            isLocationLoading = false,
                            error = "Unable to determine location"
                        )
                        Toast.makeText(context, "Unable to determine location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val countryName = address.countryName
                    val countryCode = countryCodes[countryName] ?: "ID"

                    _state.value = _state.value.copy(
                        isLocationLoading = false,
                        currentLocation = countryCode
                    )
                    onLocationReceived(countryCode)
                } else {
                    _state.value = _state.value.copy(
                        isLocationLoading = false,
                        error = "Unable to determine location"
                    )
                    Toast.makeText(context, "Unable to determine location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLocationLoading = false,
                error = "Geocoding failed: ${e.message}"
            )
            Toast.makeText(context, "Failed to get address", Toast.LENGTH_SHORT).show()
            Log.e("EditProfileViewModel", "Geocoding error", e)
        }
    }

    fun openLocationSelector(context: Context) {
        try {
            val currentLocation = _state.value.currentLocation
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$currentLocation"))
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=current+location"))
                context.startActivity(webIntent)
                Toast.makeText(context, "Select your location in the browser, then come back to manually select the country", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open maps", Toast.LENGTH_SHORT).show()
            Log.e("EditProfileViewModel", "Maps intent error", e)
        }
    }

    fun createImageCaptureIntent(context: Context): Intent? {
        return try {
            val photoFile = File(context.getExternalFilesDir(null), "profile_photo_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            if (takePictureIntent.resolveActivity(context.packageManager) != null) {
                takePictureIntent
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("EditProfileViewModel", "Error creating camera intent", e)
            null
        }
    }

    fun setSelectedImageUri(uri: Uri) {
        _state.value = _state.value.copy(selectedImageUri = uri)
    }

    fun updateProfile(
        context: Context,
        location: String? = null,
        imageUri: Uri? = null,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        _state.value = _state.value.copy(isUpdatingProfile = true)

        val token = TokenManager.getToken(context)
        if (token.isNullOrEmpty()) {
            _state.value = _state.value.copy(isUpdatingProfile = false)
            onError("No authentication token")
            return
        }

        viewModelScope.launch {
            try {
                val locationBody = location?.toRequestBody("text/plain".toMediaTypeOrNull())
                var photoPart: MultipartBody.Part? = null

                imageUri?.let { uri ->
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val file = File(context.cacheDir, "profile_photo_${System.currentTimeMillis()}.jpg")

                        inputStream?.use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }

                        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        photoPart = MultipartBody.Part.createFormData("profilePhoto", file.name, requestBody)
                    } catch (e: Exception) {
                        Log.e("EditProfileViewModel", "Error preparing image", e)
                        _state.value = _state.value.copy(isUpdatingProfile = false)
                        onError("Failed to prepare image: ${e.message}")
                        return@launch
                    }
                }

                val call = RetrofitClient.instance.updateProfile("Bearer $token", locationBody, photoPart)
                call.enqueue(object : Callback<com.example.purrytify.services.UserProfileResponse> {
                    override fun onResponse(
                        call: Call<com.example.purrytify.services.UserProfileResponse>,
                        response: Response<com.example.purrytify.services.UserProfileResponse>
                    ) {
                        Log.d("EditProfileViewModel", "Response code: ${response.code()}")
                        Log.d("EditProfileViewModel", "Response body: ${response.body()}")

                        _state.value = _state.value.copy(isUpdatingProfile = false)

                        if (response.isSuccessful) {
                            viewModelScope.launch {
                                try {
                                    val token = TokenManager.getToken(context)
                                    if (!token.isNullOrEmpty()) {
                                        val freshProfile = RetrofitClient.instance.getProfile("Bearer $token")
                                        val updatedProfile = UserProfile(
                                            id = freshProfile.id,
                                            username = freshProfile.username,
                                            email = freshProfile.email,
                                            profilePhoto = if (freshProfile.profilePhoto.isNotEmpty()) {
                                                "${RetrofitClient.getBaseUrl()}/uploads/profile-picture/${freshProfile.profilePhoto}"
                                            } else {
                                                ""
                                            },
                                            location = freshProfile.location,
                                            createdAt = freshProfile.createdAt,
                                            updatedAt = freshProfile.updatedAt
                                        )
                                        onSuccess(updatedProfile)
                                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onError("Authentication failed")
                                    }
                                } catch (e: Exception) {
                                    Log.e("EditProfileViewModel", "Error fetching updated profile", e)
                                    onError("Update successful, but failed to refresh profile data")
                                }
                            }
                        } else {
                            val errorMessage = when (response.code()) {
                                400 -> "Invalid data provided"
                                401 -> "Authentication failed"
                                413 -> "Image file too large"
                                else -> "Update failed: ${response.message()}"
                            }
                            onError(errorMessage)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<com.example.purrytify.services.UserProfileResponse>, t: Throwable) {
                        _state.value = _state.value.copy(isUpdatingProfile = false)
                        val errorMessage = "Network error: ${t.message}"
                        onError(errorMessage)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        Log.e("EditProfileViewModel", "Update profile failed", t)
                    }
                })

            } catch (e: Exception) {
                _state.value = _state.value.copy(isUpdatingProfile = false)
                val errorMessage = "Update failed: ${e.message}"
                onError(errorMessage)
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                Log.e("EditProfileViewModel", "Update profile error", e)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun getCountryNameFromCode(countryCode: String): String {
        return countryCodes.entries.find { it.value == countryCode }?.key ?: countryCode
    }

    fun getAvailableCountries(): List<Pair<String, String>> {
        return countryCodes.map { (name, code) -> name to code }
    }
}