package com.example.eventia.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
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
import com.example.eventia.ui.theme.EventiaTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.time.OffsetDateTime

@Composable
internal fun EventDetailScreen(
    authenticated: Boolean,
    onEventSignIn: (Long) -> Unit,
    onSessionExpired: (Long) -> Unit,
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onAttendanceChanged: () -> Unit,
    onVenueClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, authenticated) { viewModel.load(authenticated, onSessionExpired) }
    DisposableEffect(viewModel) {
        onDispose(viewModel::cancelRequest)
    }
    val current = state
    val visible = if (current is EventDetailUiState.Content && current.authenticated != authenticated) {
        EventDetailUiState.Loading
    } else current
    EventDetailContent(
        visible,
        onRetry = { viewModel.load(authenticated, onSessionExpired) },
        onBack = onBack,
        onVenueClick = onVenueClick,
        onAttend = { event ->
            if (authenticated) viewModel.attend(onSessionExpired, onAttendanceChanged)
            else onEventSignIn(event.id)
        },
        onRefresh = { viewModel.refresh(onSessionExpired) }
    ) { event ->
        val selected = event.id in favorites.ids
        EventCoverAction(
            favoriteIcon(selected),
            stringResource(favoriteLabel(selected)),
            onClick = { onFavoriteChange(event, !selected) },
            enabled = favorites.canChange(event.id)
        )
    }
    if (visible is EventDetailUiState.Content && visible.conflict == AttendanceConflict.Full) {
        AlertDialog(
            onDismissRequest = viewModel::dismissConflict,
            title = { Text(stringResource(R.string.event_no_spots)) },
            text = { Text(eventCapacityLabel(visible.event)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissConflict) {
                    Text(stringResource(R.string.understood))
                }
            },
            dismissButton = {
                TextButton(onClick = onExplore) { Text(stringResource(R.string.event_browse_others)) }
            }
        )
    }
}

@Composable
private fun EventDetailContent(
    state: EventDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onVenueClick: (Long) -> Unit,
    onAttend: ((Event) -> Unit)? = null,
    onRefresh: () -> Unit = {},
    favoriteAction: (@Composable (Event) -> Unit)? = null
) {
    if (state is EventDetailUiState.Content) {
        EventDetails(state, onBack, onVenueClick, onAttend, onRefresh, favoriteAction)
        return
    }

    Scaffold(topBar = {
        Row(Modifier.statusBarsPadding().padding(horizontal = 8.dp)) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.navigate_back))
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                EventDetailUiState.Loading -> LoadingScreen(stringResource(R.string.event_detail_loading))
                EventDetailUiState.Empty -> ScreenMessage(
                    title = stringResource(R.string.event_detail_empty),
                    actionLabel = stringResource(R.string.navigate_back),
                    onAction = onBack
                )
                EventDetailUiState.Error -> ScreenMessage(
                    title = stringResource(R.string.event_detail_error),
                    isError = true,
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRetry
                )
                is EventDetailUiState.Content -> Unit
            }
        }
    }
}

@Composable
private fun EventDetails(
    state: EventDetailUiState.Content,
    onBack: () -> Unit,
    onVenueClick: (Long) -> Unit,
    onAttend: ((Event) -> Unit)?,
    onRefresh: () -> Unit,
    favoriteAction: (@Composable (Event) -> Unit)? = null
) {
    val event = state.event
    val point = MapPoint.from(event.venue.latitude, event.venue.longitude)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val accent = MaterialTheme.colorScheme.primary
    val dateTime = eventLocalDateTime(event)
    val shareLabel = stringResource(R.string.event_share)
    val shareText = stringResource(R.string.event_share_text, event.title, dateTime, event.venue.name)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            EventArtwork(event.imageUrl, Modifier.fillMaxWidth().aspectRatio(5f / 3f))
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EventCoverAction(R.drawable.ic_back, stringResource(R.string.navigate_back), onBack)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    EventCoverAction(R.drawable.ic_event_share, shareLabel, onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, shareLabel))
                    })
                    favoriteAction?.invoke(event)
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                EventCategoryLabel(event.category.name)
                Text(event.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventDetailIcon(R.drawable.ic_event_calendar)
                    Text(dateTime, style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { onVenueClick(event.venue.id) }
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventDetailIcon(R.drawable.ic_map)
                    Text(
                        event.venue.name,
                        Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                    Icon(
                        painterResource(R.drawable.ic_back),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = 180f },
                        tint = accent
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val hasCapacity = event.capacity > 0
                Text(
                    eventCapacityLabel(event),
                    style = MaterialTheme.typography.bodySmall
                )
                if (hasCapacity) {
                    LinearProgressIndicator(
                        progress = { (event.availableSpots.toFloat() / event.capacity).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            EventDescription(event)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.venue_location), style = MaterialTheme.typography.titleSmall)
                EventLocationPreview(event.venue, point)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        if (point != null) {
                            val coordinates = "${point.latitude},${point.longitude}"
                            val query = Uri.encode("$coordinates(${event.venue.name})")
                            try {
                                uriHandler.openUri("geo:$coordinates?q=$query")
                            } catch (_: IllegalArgumentException) {
                                Toast.makeText(
                                    context, R.string.venue_directions_unavailable, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = point != null,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.venue_directions))
                }
                Button(
                    onClick = { onAttend?.invoke(event) },
                    enabled = onAttend != null && !state.updating && !state.refreshRequired &&
                        (!state.authenticated || event.isAttending != null),
                    modifier = Modifier.weight(1.15f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(when {
                        state.updating -> R.string.event_attendance_updating
                        event.isAttending == true -> R.string.event_cancel_attendance
                        else -> R.string.event_attend
                    }))
                }
            }
            if (!state.updating) {
                when (state.conflict) {
                    AttendanceConflict.AlreadyRegistered ->
                        Text(stringResource(R.string.event_already_registered))
                    AttendanceConflict.PendingRegistration, AttendanceConflict.Unconfirmed ->
                        Text(stringResource(R.string.event_attendance_conflict), color = MaterialTheme.colorScheme.error)
                    else -> if (state.refreshRequired) {
                        val message = if (state.attendanceError) R.string.event_attendance_error
                        else R.string.event_detail_error
                        Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                    }
                }
                if (state.refreshRequired) {
                    TextButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}

@Composable
private fun eventCapacityLabel(event: Event): String = if (event.capacity > 0) {
    stringResource(R.string.event_capacity, event.availableSpots, event.capacity)
} else {
    stringResource(R.string.available_spots, event.availableSpots)
}

@Composable
private fun EventCoverAction(icon: Int, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
            Icon(
                painterResource(icon),
                contentDescription = label,
                modifier = Modifier.padding(7.dp).size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
            )
        }
    }
}

@Composable
private fun EventDetailIcon(icon: Int) {
    Icon(
        painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EventDescription(event: Event) {
    val description = event.description?.takeIf { it.isNotBlank() } ?: event.summary
    var expanded by rememberSaveable(event.id, description) { mutableStateOf(false) }
    var overflows by remember(event.id, description) { mutableStateOf(false) }
    if (description.isBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.event_description_title), style = MaterialTheme.typography.titleSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow }
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.event_read_less else R.string.event_read_more))
            }
        }
    }
}

@Composable
private fun EventLocationPreview(venue: Venue, point: MapPoint?) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val available = remember(context, inspection) {
        !inspection && !context.packageManager.getApplicationInfo(
            context.packageName, PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData?.getString("com.google.android.geo.API_KEY").isNullOrBlank() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    val shape = RoundedCornerShape(12.dp)
    if (point != null && available) {
        val location = LatLng(point.latitude, point.longitude)
        val camera = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(location, 16f)
        }
        GoogleMap(
            modifier = Modifier.fillMaxWidth().height(80.dp).clip(shape),
            cameraPositionState = camera,
            googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
            uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false)
        ) {
            Marker(state = rememberUpdatedMarkerState(location), title = venue.name)
        }
    } else {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = shape) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventDetailIcon(R.drawable.ic_map)
                Text(
                    venue.address.ifBlank { stringResource(R.string.venue_address_unavailable) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EventDetailPreview() {
    EventiaTheme {
        Surface {
            EventDetailContent(
                state = EventDetailUiState.Content(Event(
                    id = 1,
                    title = "Noche de jazz en la plaza",
                    summary = "Un recorrido por los clásicos del jazz con músicos invitados de la región. Trae abrigo para disfrutar de la música al aire libre.",
                    description = null,
                    startsAt = OffsetDateTime.parse("2026-11-14T20:00:00-04:00"),
                    endsAt = null,
                    category = Category(1, "Música"),
                    imageUrl = null,
                    venue = Venue(1, "Casa de la Cultura", "Plaza principal", latitude = -16.5, longitude = -68.15),
                    capacity = 50,
                    availableSpots = 36,
                    isAttending = false
                )),
                onRetry = {},
                onBack = {},
                onVenueClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailErrorPreview() {
    EventiaTheme {
        EventDetailContent(EventDetailUiState.Error, onRetry = {}, onBack = {}, onVenueClick = {})
    }
}
