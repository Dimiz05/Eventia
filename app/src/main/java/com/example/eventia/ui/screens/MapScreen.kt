package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.ui.theme.EventiaTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay

private val TestMarker = LatLng(-16.4957, -68.1336)
private const val MapLoadTimeoutMillis = 20_000L

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        MapUiState.NotConfigured -> MapMessage(R.string.map_not_configured)
        MapUiState.ServicesUnavailable -> MapMessage(R.string.map_services_unavailable, viewModel::retry)
        is MapUiState.Ready -> key(current.attempt) { TestMap(viewModel::retry) }
    }
}

@Composable
private fun TestMap(onRetry: () -> Unit) {
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(TestMarker, 14f)
    }
    var loaded by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }

    LaunchedEffect(loaded) {
        if (!loaded) {
            delay(MapLoadTimeoutMillis)
            timedOut = true
        }
    }
    if (timedOut) {
        MapMessage(R.string.map_load_error, onRetry)
        return
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            onMapLoaded = { loaded = true }
        ) {
            Marker(
                state = rememberUpdatedMarkerState(position = TestMarker),
                title = stringResource(R.string.map_marker_title)
            )
        }
        if (!loaded) {
            Surface {
                LoadingScreen(stringResource(R.string.map_loading))
            }
        }
    }
}

@Composable
private fun MapMessage(message: Int, onRetry: (() -> Unit)? = null) {
    ScreenMessage(
        title = stringResource(message),
        isError = true,
        actionLabel = stringResource(R.string.retry),
        onAction = onRetry
    )
}

@Preview(showBackground = true)
@Composable
private fun MapErrorPreview() {
    EventiaTheme { MapMessage(R.string.map_load_error, onRetry = {}) }
}
