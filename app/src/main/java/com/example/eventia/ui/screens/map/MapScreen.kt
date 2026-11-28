package com.example.eventia.ui.screens.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.MapSkeleton
import com.example.eventia.ui.components.FavoriteButton
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.screens.explore.ExploreSearch
import com.example.eventia.ui.screens.explore.categoryIcon
import com.example.eventia.ui.screens.explore.exploreColors
import com.example.eventia.ui.theme.EventiaTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.time.OffsetDateTime
import kotlinx.coroutines.delay

private val DefaultMapCenter = LatLng(-16.4957, -68.1336)
private const val MapLoadTimeoutMillis = 20_000L
private const val SelectedMarkerZoom = 17f

@Composable
fun MapScreen(
    onEventClick: (Long) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
    favoriteAction: (@Composable (Event) -> Unit)? = null,
    avatar: @Composable () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = exploreColors()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            when (val current = state) {
                MapUiState.NotConfigured -> MapMessage(R.string.map_not_configured)
                MapUiState.ServicesUnavailable -> MapMessage(R.string.map_services_unavailable, viewModel::retryMap)
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
                        },
                        favoriteAction = favoriteAction,
                        avatar = avatar,
                        userLocation = userLocation,
                        onUserLocationChanged = viewModel::updateUserLocation
                    )
                }
            }
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
    onEventClick: (Long) -> Unit,
    favoriteAction: (@Composable (Event) -> Unit)?,
    userLocation: MapUserLocation? = null,
    onUserLocationChanged: (MapUserLocation?) -> Unit = {},
    avatar: @Composable () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var satellite by rememberSaveable { mutableStateOf(false) }
    val markers = remember(state.markers, query) { filterMapMarkers(state.markers, query) }
    val selectedEvents = state.selectedPoint?.let(markers::get).orEmpty()
    val hasSelection = selectedEvents.isNotEmpty()
    val waitsForEvents = state.isLoading && state.markers.isEmpty()
    val eventLoadFailed = state.hasError && state.markers.isEmpty()
    val showMap = !waitsForEvents && !eventLoadFailed
    val initialPoint = state.selectedPoint ?: state.markers.keys.firstOrNull()
    val camera = key(showMap) {
        rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialPoint?.toLatLng() ?: DefaultMapCenter, 14f)
        }
    }
    val focus = LocalFocusManager.current
    val preview = LocalInspectionMode.current
    var loaded by remember(showMap) { mutableStateOf(preview) }
    var timedOut by remember(showMap) { mutableStateOf(false) }

    LaunchedEffect(showMap, loaded) {
        if (showMap && !loaded) {
            delay(MapLoadTimeoutMillis)
            timedOut = true
        }
    }
    BackHandler(enabled = hasSelection) { onPointSelected(null) }

    val (precision, centerOnLocation) = if (preview) null to {} else rememberMapLocation(
        onAccessChanged = { access ->
            if (userLocation != null && userLocation.precise != access) onUserLocationChanged(null)
        },
        onPosition = { location ->
            onUserLocationChanged(location)
            if (loaded && !timedOut) {
                val zoom = maxOf(camera.position.zoom, 15f)
                camera.move(CameraUpdateFactory.newLatLngZoom(location.point.toLatLng(), zoom))
            }
        }
    )
    val accessibleLocation = userLocation?.takeIf { it.precise == precision }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var toolbarHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val margin = with(density) { 20.dp.roundToPx() }
    val mapTop = toolbarHeight.coerceAtMost(mapSize.height / 2)
    val mapPadding = PaddingValues(top = with(density) { mapTop.toDp() })
    val projection = camera.projection
    val cameraMoving = camera.isMoving
    val outsideResults = remember(camera.position, cameraMoving, projection, markers, mapSize, mapTop) {
        !cameraMoving && markers.isNotEmpty() && mapSize.height > mapTop && projection != null &&
            markers.keys.none { point ->
                val pixel = projection.toScreenLocation(point.toLatLng())
                pixel.x in 0 until mapSize.width && pixel.y in mapTop until mapSize.height
            }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelMaxHeight = maxHeight * 0.52f
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f).onSizeChanged { mapSize = it }) {
                when {
                    waitsForEvents -> MapSkeleton(stringResource(R.string.catalog_loading))
                    eventLoadFailed -> MapMessage(R.string.catalog_error, onRetryEvents)
                    timedOut -> MapMessage(R.string.map_load_error, onRetryMap)
                    else -> {
                        if (preview) {
                            MapPreviewBackdrop(hasSelection)
                        } else {
                            EventMap(
                                markers = markers,
                                selectedPoint = state.selectedPoint,
                                camera = camera,
                                satellite = satellite,
                                locationEnabled = accessibleLocation != null,
                                contentPadding = mapPadding,
                                onLoaded = { loaded = true },
                                onPointSelected = { point ->
                                    focus.clearFocus()
                                    onPointSelected(point)
                                    val zoom = maxOf(camera.position.zoom, SelectedMarkerZoom)
                                    camera.move(CameraUpdateFactory.newLatLngZoom(point.toLatLng(), zoom))
                                },
                                onMapClick = {
                                    focus.clearFocus()
                                    onPointSelected(null)
                                }
                            )
                        }
                        if (!loaded) {
                            Surface(Modifier.fillMaxSize()) {
                                MapSkeleton(stringResource(R.string.map_loading))
                            }
                        }
                    }
                }
                if (loaded && !timedOut && showMap) {
                    Column(Modifier.align(Alignment.TopCenter).onSizeChanged { toolbarHeight = it.height }.padding(top = 8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExploreSearch(query, onQueryChange = { value ->
                                query = value
                                onPointSelected(null)
                            }, label = stringResource(R.string.map_search), inlineHint = true,
                                modifier = Modifier.weight(1f))
                            avatar()
                        }
                        if (state.categories.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    MapCategoryChip(stringResource(R.string.map_all_categories), R.drawable.ic_category_all, state.categoryId == null) {
                                        onCategorySelected(null)
                                    }
                                }
                                items(state.categories, key = { it.id }) { category ->
                                    MapCategoryChip(category.name, categoryIcon(category.name), state.categoryId == category.id) {
                                        onCategorySelected(category.id)
                                    }
                                }
                            }
                        }
                        if (state.hasError || markers.isEmpty()) {
                            Surface(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surface
                            ) {
                                if (state.hasError) {
                                    TextButton(onClick = onRetryEvents) { Text(stringResource(R.string.map_retry_events)) }
                                } else {
                                    Text(
                                        stringResource(if (query.isBlank()) R.string.map_events_empty else R.string.map_search_empty),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    if (outsideResults) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 88.dp, bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(stringResource(R.string.map_no_events_in_area), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    focus.clearFocus()
                                    val points = markers.keys.map { it.toLatLng() }
                                    val update = if (points.size == 1) {
                                        CameraUpdateFactory.newLatLngZoom(points.single(), 15f)
                                    } else {
                                        val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
                                        val padding = minOf(margin, (minOf(mapSize.width, mapSize.height - mapTop) - 1) / 4)
                                        CameraUpdateFactory.newLatLngBounds(bounds, padding)
                                    }
                                    camera.move(update)
                                }) { Text(stringResource(R.string.map_frame_events)) }
                            }
                        }
                    }
                    MapControls(
                        satellite = satellite,
                        onToggleLayer = { satellite = !satellite },
                        onRecenter = centerOnLocation,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    )
                }
            }

            if (hasSelection && loaded && !timedOut) {
                key(state.selectedPoint) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = colors.surface,
                        shadowElevation = 6.dp
                    ) {
                        MapEventSheet(
                            selectedEvents,
                            onDismiss = { onPointSelected(null) },
                            onEventClick = onEventClick,
                            favoriteAction = favoriteAction,
                            userLocation = accessibleLocation,
                            modifier = Modifier.heightIn(max = panelMaxHeight)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapCategoryChip(label: String, icon: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                painterResource(icon), null, Modifier.size(16.dp),
                tint = if (selected) colors.onPrimary else colors.primary
            )
        },
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surface,
            labelColor = colors.onSurface,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary
        )
    )
}

@Composable
private fun MapControls(satellite: Boolean, onToggleLayer: () -> Unit, onRecenter: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = colors.surface, shadowElevation = 4.dp) {
            IconButton(onClick = onToggleLayer) {
                Icon(
                    painterResource(R.drawable.ic_map_layers),
                    stringResource(if (satellite) R.string.map_standard_layer else R.string.map_satellite_layer),
                    tint = colors.primary
                )
            }
        }
        Surface(shape = CircleShape, color = colors.primary, shadowElevation = 4.dp) {
            IconButton(onClick = onRecenter, modifier = Modifier.size(54.dp)) {
                Icon(painterResource(R.drawable.ic_map_center), stringResource(R.string.map_recenter), tint = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun EventMap(
    markers: Map<MapPoint, List<Event>>,
    selectedPoint: MapPoint?,
    camera: CameraPositionState,
    satellite: Boolean,
    locationEnabled: Boolean,
    contentPadding: PaddingValues,
    onLoaded: () -> Unit,
    onPointSelected: (MapPoint) -> Unit,
    onMapClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        contentPadding = contentPadding,
        properties = MapProperties(
            mapType = if (satellite) MapType.SATELLITE else MapType.NORMAL,
            isMyLocationEnabled = locationEnabled
        ),
        uiSettings = MapUiSettings(
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false
        ),
        onMapLoaded = onLoaded,
        onMapClick = { onMapClick() }
    ) {
        markers.forEach { (point, events) ->
            key(point) {
                val selected = point == selectedPoint
                val title = events.first().title
                MarkerComposable(
                    selected, title, colors.primary, colors.surface,
                    state = rememberUpdatedMarkerState(point.toLatLng()),
                    anchor = Offset(0.5f, 1f),
                    title = title,
                    contentDescription = title,
                    zIndex = if (selected) 1f else 0f,
                    onClick = {
                        onPointSelected(point)
                        true
                    }
                ) { MapPin(selected) }
            }
        }
    }
}

@Composable
private fun MapPin(selected: Boolean) {
    val pinColor = MaterialTheme.colorScheme.primary
    val diameter = if (selected) 48.dp else 36.dp
    Box(Modifier.size(width = diameter + 8.dp, height = diameter + 12.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val tip = Path().apply {
                moveTo(center.x - 6.dp.toPx(), size.height - 14.dp.toPx())
                lineTo(center.x + 6.dp.toPx(), size.height - 14.dp.toPx())
                lineTo(center.x, size.height)
                close()
            }
            drawPath(tip, pinColor)
        }
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(diameter),
            shape = CircleShape,
            color = pinColor,
            shadowElevation = if (selected) 4.dp else 1.dp,
            border = BorderStroke(if (selected) 4.dp else 1.5.dp, Color.White)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_catalog_ticket), null,
                    Modifier.size(if (selected) 24.dp else 18.dp), tint = Color.White
                )
            }
        }
    }
}

private fun MapPoint.toLatLng() = LatLng(latitude, longitude)

@Composable
private fun MapMessage(message: Int, onRetry: (() -> Unit)? = null) {
    ScreenMessage(
        title = stringResource(message),
        isError = true,
        actionLabel = stringResource(R.string.retry),
        onAction = onRetry
    )
}

@Composable
private fun MapPreviewBackdrop(hasSelection: Boolean) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.surfaceContainer)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(colors.primaryContainer, Offset(size.width * 0.06f, size.height * 0.2f), Size(size.width * 0.3f, size.height * 0.26f))
            val river = Path().apply {
                moveTo(-10f, size.height * 0.88f)
                cubicTo(size.width * 0.25f, size.height * 0.73f, size.width * 0.65f, size.height * 0.78f, size.width * 1.1f, size.height * 0.42f)
            }
            drawPath(river, colors.primaryContainer, style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Round))
            for (index in 1..4) {
                val x = size.width * index / 5
                val y = size.height * index / 5
                drawLine(colors.surface, Offset(x, 0f), Offset(x - size.width * 0.12f, size.height), 7.dp.toPx())
                drawLine(colors.surface, Offset(0f, y), Offset(size.width, y + size.height * 0.12f), 8.dp.toPx())
            }
        }
        Box(Modifier.offset(x = maxWidth * 0.68f, y = maxHeight * 0.32f)) { MapPin(false) }
        Box(Modifier.offset(x = maxWidth * 0.17f, y = maxHeight * 0.65f)) { MapPin(false) }
        Box(Modifier.align(Alignment.Center)) { MapPin(hasSelection) }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 850)
@Composable
private fun MapSelectedPreview() {
    val music = Category(1, "Música")
    val event = Event(
        id = 1,
        title = "Noche de jazz en la plaza",
        summary = "Jazz en directo en el patio principal.",
        description = null,
        startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-04:00"),
        endsAt = null,
        category = music,
        imageUrl = null,
        venue = Venue(1, "Casa de la Cultura", "Av. Principal 123", latitude = -16.5, longitude = -68.15),
        capacity = 50,
        availableSpots = 36,
        isAttending = null
    )
    val point = MapPoint(-16.5, -68.15)
    CompositionLocalProvider(LocalInspectionMode provides true) {
        EventiaTheme {
            MaterialTheme(colorScheme = exploreColors()) {
                Column(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding().navigationBarsPadding()
                ) {
                            MapContent(
                        state = MapUiState.Ready(
                            attempt = 0,
                            categoryId = null,
                            categories = listOf(music, Category(2, "Cultura"), Category(3, "Tecnología")),
                            markers = mapOf(point to listOf(event)),
                            selectedPoint = point,
                            isLoading = false
                        ),
                        onCategorySelected = {},
                        onPointSelected = {},
                        onRetryMap = {},
                        onRetryEvents = {},
                        onEventClick = {},
                        favoriteAction = { FavoriteButton(false, true) {} }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MapErrorPreview() {
    EventiaTheme { MapMessage(R.string.map_load_error, onRetry = {}) }
}
