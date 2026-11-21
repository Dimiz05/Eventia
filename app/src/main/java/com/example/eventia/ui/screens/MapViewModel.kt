package com.example.eventia.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface MapUiState {
    data object NotConfigured : MapUiState
    data object ServicesUnavailable : MapUiState
    data class Ready(val attempt: Int) : MapUiState
}

@HiltViewModel
class MapViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    private var attempt = 0
    private val _state = MutableStateFlow(checkAvailability())
    val state = _state.asStateFlow()

    fun retry() {
        attempt++
        _state.value = checkAvailability()
    }

    private fun checkAvailability(): MapUiState {
        val metadata = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData
        if (metadata?.getString("com.google.android.geo.API_KEY").isNullOrBlank()) {
            return MapUiState.NotConfigured
        }
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return if (availability == ConnectionResult.SUCCESS) {
            MapUiState.Ready(attempt)
        } else {
            MapUiState.ServicesUnavailable
        }
    }
}
