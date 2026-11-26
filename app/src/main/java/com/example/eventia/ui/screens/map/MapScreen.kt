package com.example.eventia.ui.screens.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.FavoriteButton
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.screens.explore.ExploreBadge
import com.example.eventia.ui.screens.explore.ExploreSearch
import com.example.eventia.ui.screens.explore.exploreColors
import com.example.eventia.ui.theme.EventiaTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
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
    favoriteAction: (@Composable (Event) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = exploreColors()) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            MapHeader()
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
                        favoriteAction = favoriteAction
                    )
                }
            }
        }
    }
}

@Composable
private fun MapHeader() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painterResource(R.drawable.eventia_app_icon),
            contentDescription = null,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
        )
        Column {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
            Text(
                stringResource(R.string.tab_map),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = colors.outlineVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapContent(
    state: MapUiState.Ready,
    onCategorySelected: (Long?) -> Unit,
    onPointSelected: (MapPoint?) -> Unit,
    onRetryMap: () -> Unit,
    onRetryEvents: () -> Unit,
    onEventClick: (Long) -> Unit,
    favoriteAction: (@Composable (Event) -> Unit)?
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        val peekHeight = if (hasSelection) minOf(300.dp, availableHeight * 0.6f) else 0.dp
        BottomSheetScaffold(
            sheetPeekHeight = peekHeight,
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetContainerColor = colors.surface,
            sheetShadowElevation = if (hasSelection) 8.dp else 0.dp,
            sheetSwipeEnabled = hasSelection,
            sheetDragHandle = if (hasSelection) {
                { BottomSheetDefaults.DragHandle(color = colors.outlineVariant) }
            } else null,
            sheetContent = {
                if (hasSelection) {
                    MapEventSheet(
                        selectedEvents,
                        onDismiss = { onPointSelected(null) },
                        onEventClick = onEventClick,
                        favoriteAction = favoriteAction,
                        modifier = Modifier.heightIn(max = availableHeight * 0.73f)
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
                when {
                    waitsForEvents -> LoadingScreen(stringResource(R.string.catalog_loading))
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
                                onLoaded = { loaded = true },
                                onPointSelected = { point ->
                                    focus.clearFocus()
                                    onPointSelected(point)
                                },
                                onMapClick = {
                                    focus.clearFocus()
                                    onPointSelected(null)
                                }
                            )
                        }
                        if (!loaded) {
                            Surface(Modifier.fillMaxSize()) {
                                LoadingScreen(stringResource(R.string.map_loading))
                            }
                        }
                    }
                }
                if (loaded && !timedOut && showMap) {
                    Column(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                        ExploreSearch(query, onQueryChange = { value ->
                            query = value
                            onPointSelected(null)
                        }, label = stringResource(R.string.map_search))
                        if (state.categories.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    MapCategoryChip(stringResource(R.string.map_all_categories), state.categoryId == null) {
                                        onCategorySelected(null)
                                    }
                                }
                                items(state.categories, key = { it.id }) { category ->
                                    MapCategoryChip(category.name, state.categoryId == category.id) {
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
                    MapControls(
                        satellite = satellite,
                        onToggleLayer = { satellite = !satellite },
                        onRecenter = {
                            val point = state.selectedPoint?.takeIf(markers::containsKey) ?: markers.keys.firstOrNull()
                            if (!preview) {
                                camera.move(CameraUpdateFactory.newLatLngZoom(point?.toLatLng() ?: DefaultMapCenter, 14f))
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MapCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
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
    onLoaded: () -> Unit,
    onPointSelected: (MapPoint) -> Unit,
    onMapClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        properties = MapProperties(mapType = if (satellite) MapType.SATELLITE else MapType.NORMAL),
        uiSettings = MapUiSettings(mapToolbarEnabled = false),
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
                    title = title,
                    contentDescription = title,
                    zIndex = if (selected) 1f else 0f,
                    onClick = { marker ->
                        val zoom = maxOf(camera.position.zoom, SelectedMarkerZoom)
                        camera.move(CameraUpdateFactory.newLatLngZoom(marker.position, zoom))
                        onPointSelected(point)
                        true
                    }
                ) { MapPin(title, selected) }
            }
        }
    }
}

@Composable
private fun MapPin(title: String, selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (selected) {
            Surface(
                modifier = Modifier.padding(bottom = 4.dp),
                shape = CircleShape,
                color = colors.surface
            ) {
                Text(
                    title,
                    modifier = Modifier.widthIn(max = 168.dp).padding(horizontal = 10.dp, vertical = 5.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = if (selected) colors.primary else colors.onPrimaryContainer,
            border = BorderStroke(2.dp, colors.surface)
        ) {
            Icon(
                painterResource(R.drawable.ic_catalog_ticket),
                contentDescription = null,
                modifier = Modifier.size(if (selected) 44.dp else 36.dp).padding(10.dp),
                tint = if (selected) colors.onPrimary else colors.primaryContainer
            )
        }
        Box(Modifier.offset(y = (-2).dp).rotate(45f).size(7.dp).background(colors.primary))
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
        Box(Modifier.offset(x = maxWidth * 0.68f, y = maxHeight * 0.32f)) { MapPin("Feria del Libro", false) }
        Box(Modifier.offset(x = maxWidth * 0.17f, y = maxHeight * 0.65f)) { MapPin("Encuentro de tecnología", false) }
        Box(Modifier.align(Alignment.Center)) { MapPin("Noche de jazz", hasSelection) }
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
                    MapHeader()
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
