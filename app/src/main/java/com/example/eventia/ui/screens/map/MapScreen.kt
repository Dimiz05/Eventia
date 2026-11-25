package com.example.eventia.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.components.EventCategoryLabel
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.theme.EventiaTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay

private val DefaultMapCenter = LatLng(-16.4957, -68.1336)
private const val MapLoadTimeoutMillis = 20_000L
private const val SelectedMarkerZoom = 17f

@Composable
fun MapScreen(onEventClick: (Long) -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val current = state) {
        MapUiState.NotConfigured -> MapMessage(R.string.map_not_configured)
        MapUiState.ServicesUnavailable ->
            MapMessage(R.string.map_services_unavailable, viewModel::retryMap)
        is MapUiState.Ready -> key(current.attempt) {
            MapContent(
                state = current,
                onCategorySelected = viewModel::selectCategory,
                onPointSelected = viewModel::selectPoint,
                onRetryMap = viewModel::retryMap,
                onRetryEvents = viewModel::retryEvents,
                onEventClick = { id ->
                    viewModel.selectPoint(null)
                    onEventClick(id)
                }
            )
        }
    }
}

@Composable
private fun MapContent(
    state: MapUiState.Ready,
    onCategorySelected: (Long?) -> Unit,
    onPointSelected: (MapPoint?) -> Unit,
    onRetryMap: () -> Unit,
    onRetryEvents: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    val selectedEvents = state.selectedPoint?.let(state.markers::get)
    Column(Modifier.fillMaxSize()) {
        if (state.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text(stringResource(R.string.map_all_categories)) }
                    )
                }
                items(state.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = state.categoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }
        }
        when {
            state.isLoading && state.markers.isEmpty() ->
                LoadingScreen(stringResource(R.string.catalog_loading))
            state.hasError && state.markers.isEmpty() ->
                MapMessage(R.string.catalog_error, onRetryEvents)
            else -> {
                if (state.hasError) {
                    TextButton(onClick = onRetryEvents) {
                        Text(stringResource(R.string.map_retry_events))
                    }
                }
                if (state.markers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.map_events_empty),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                EventMap(state, onPointSelected, onRetryMap)
            }
        }
    }
    if (selectedEvents != null) {
        EventSummarySheet(selectedEvents, { onPointSelected(null) }, onEventClick)
    }
}

@Composable
private fun EventMap(
    state: MapUiState.Ready,
    onPointSelected: (MapPoint) -> Unit,
    onRetry: () -> Unit
) {
    val camera = rememberCameraPositionState {
        val first = state.markers.keys.firstOrNull()
        val center = first?.let { LatLng(it.latitude, it.longitude) } ?: DefaultMapCenter
        position = CameraPosition.fromLatLngZoom(center, 14f)
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
            state.markers.forEach { (point, events) ->
                key(point) {
                    val selected = point == state.selectedPoint
                    val hue = if (selected) {
                        BitmapDescriptorFactory.HUE_AZURE
                    } else {
                        BitmapDescriptorFactory.HUE_RED
                    }
                    Marker(
                        state = rememberUpdatedMarkerState(LatLng(point.latitude, point.longitude)),
                        title = events.first().title,
                        icon = remember(hue) { BitmapDescriptorFactory.defaultMarker(hue) },
                        zIndex = if (selected) 1f else 0f,
                        onClick = { marker ->
                            val zoom = maxOf(camera.position.zoom, SelectedMarkerZoom)
                            camera.move(CameraUpdateFactory.newLatLngZoom(marker.position, zoom))
                            onPointSelected(point)
                            true
                        }
                    )
                }
            }
        }
        if (!loaded) {
            Surface {
                LoadingScreen(stringResource(R.string.map_loading))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventSummarySheet(
    events: List<Event>,
    onDismiss: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (events.size > 1) {
                item {
                    Text(
                        text = stringResource(R.string.map_shared_point),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            items(events.size) { index ->
                val event = events[index]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (index > 0) HorizontalDivider()
                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                    Text(eventLocalDateTime(event), style = MaterialTheme.typography.bodyMedium)
                    EventCategoryLabel(event.category.name)
                    if (event.summary.isNotBlank()) {
                        Text(event.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(onClick = { onEventClick(event.id) }) {
                        Text(stringResource(R.string.map_view_detail))
                    }
                }
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
