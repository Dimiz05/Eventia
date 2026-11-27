package com.example.eventia.ui.screens.eventdetail

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.eventia.domain.model.hasStarted
import com.example.eventia.ui.components.DetailLocationMap
import com.example.eventia.ui.components.DetailTopBar
import com.example.eventia.ui.components.DetailArtwork
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.components.favoriteIcon
import com.example.eventia.ui.components.favoriteLabel
import com.example.eventia.ui.components.rememberVenueDirections
import com.example.eventia.ui.screens.favorites.FavoritesUiState
import com.example.eventia.ui.screens.map.MapPoint
import com.example.eventia.ui.screens.explore.exploreColors
import com.example.eventia.ui.screens.explore.ExploreBadge
import com.example.eventia.ui.theme.EventiaTheme
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val EventIvory = Color(0xFFFAF6EF)
private val EventSage = Color(0xFFD5E4B5)
private val EventForest = Color(0xFF234617)
private val EventCoral = Color(0xFFC84639)
private val EventSecondary = Color(0xFF575E50)
private val EventCoverHeight = 300.dp
private val EventPanelOverlap = 24.dp

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
            else if (!event.hasStarted(Instant.now())) onEventSignIn(event.id)
        },
        onRefresh = { viewModel.refresh(onSessionExpired) }
    ) { event ->
        val selected = event.id in favorites.ids
        EventCoverAction(
            favoriteIcon(selected),
            stringResource(favoriteLabel(selected)),
            onClick = { onFavoriteChange(event, !selected) },
            enabled = favorites.canChange(event.id),
            tint = if (selected) EventCoral else EventForest
        )
    }
    if (visible is EventDetailUiState.Content && visible.conflict == AttendanceConflict.Full &&
        !visible.event.hasStarted(Instant.now())
    ) {
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

    MaterialTheme(colorScheme = exploreColors()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { DetailTopBar(stringResource(R.string.detail_event), onBack) }
        ) { padding ->
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
    val colors = exploreColors().copy(
        primary = EventForest, onPrimary = Color.White,
        primaryContainer = EventSage, onPrimaryContainer = EventForest,
        surface = EventIvory, onSurface = EventForest, onSurfaceVariant = EventSecondary
    )
    val dateTime = eventLocalDateTime(event)
    val shareLabel = stringResource(R.string.event_share)
    val shareText = stringResource(
        R.string.event_share_text, event.title, dateTime, event.venue.name, venueMapUrl(event.venue)
    )
    val openDirections = rememberVenueDirections(event.venue, point)
    val attendanceClosed by produceState(event.hasStarted(Instant.now()), event.startsAt) {
        var now = Instant.now()
        while (!event.hasStarted(now)) {
            value = false
            val millisUntilStart = event.startsAt.toInstant().toEpochMilli() - now.toEpochMilli()
            delay((millisUntilStart + 1).coerceIn(1L, 60_000L))
            now = Instant.now()
        }
        value = true
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = colors.surface,
            bottomBar = {
                EventAttendanceBar(
                    event = event,
                    state = state,
                    attendanceClosed = attendanceClosed,
                    onAttend = onAttend,
                    onRefresh = onRefresh
                )
            }
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(Modifier.fillMaxWidth().height(EventCoverHeight)) {
                    DetailArtwork(event.imageUrl, R.drawable.ic_catalog_ticket, Modifier.fillMaxSize())
                    Box(
                        Modifier.fillMaxWidth().height(96.dp).background(
                            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.48f), Color.Transparent))
                        )
                    )
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EventCoverAction(R.drawable.ic_back, stringResource(R.string.navigate_back), onBack)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            favoriteAction?.invoke(event)
                            EventCoverAction(R.drawable.ic_event_share, shareLabel, onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, shareLabel))
                            })
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = EventCoverHeight - EventPanelOverlap),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        EventHeading(event)
                        HorizontalDivider(color = EventForest.copy(alpha = 0.54f))
                        EventDescription(event)
                        EventLocation(event, point, onVenueClick, openDirections)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventHeading(event: Event) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ExploreBadge(event.category.name, containerColor = EventSage, contentColor = EventForest)
            Text(
                event.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EventForest
            )
        }
        EventDateBadge(event)
    }
}

@Composable
private fun EventDateBadge(event: Event) {
    val locale = LocalConfiguration.current.locales[0]
    val start = event.startsAt.atZoneSameInstant(ZoneId.systemDefault())
    val day = start.format(DateTimeFormatter.ofPattern("d", locale))
    val month = start.format(DateTimeFormatter.ofPattern("MMM", locale)).trimEnd('.').uppercase(locale)
    val time = start.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val monthEnd = with(density) { (typography.headlineLarge.lineHeight.toPx() + typography.labelLarge.lineHeight.toPx() + 4.dp.toPx()) }
    val transition = with(density) { 14.dp.toPx() }
    Column(
        Modifier.widthIn(min = 88.dp).clip(RoundedCornerShape(16.dp)).background(
            Brush.verticalGradient(
                colors = listOf(EventSage, EventForest),
                startY = monthEnd,
                endY = monthEnd + transition
            )
        ).padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day, style = typography.headlineLarge, fontWeight = FontWeight.Bold, color = EventForest)
        Text(month, style = typography.labelLarge, fontWeight = FontWeight.Bold, color = EventForest)
        Spacer(Modifier.height(14.dp))
        Text(time, style = typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun EventLocation(
    event: Event,
    point: MapPoint?,
    onVenueClick: (Long) -> Unit,
    openDirections: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.venue_location),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable { onVenueClick(event.venue.id) }
                .heightIn(min = 48.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(24.dp), tint = EventForest)
            Column(Modifier.weight(1f)) {
                Text(event.venue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (event.venue.address.isNotBlank()) {
                    Text(event.venue.address, style = MaterialTheme.typography.bodyMedium, color = EventSecondary)
                }
            }
            Icon(
                painterResource(R.drawable.ic_back), null,
                Modifier.size(16.dp).graphicsLayer { rotationZ = 180f },
                tint = EventForest
            )
        }
        DetailLocationMap(event.venue, point, Modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(16.dp)))
        OutlinedButton(
            onClick = openDirections,
            enabled = point != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, EventForest.copy(alpha = if (point != null) 1f else 0.38f))
        ) {
            Text(stringResource(R.string.venue_directions), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EventAttendanceBar(
    event: Event,
    state: EventDetailUiState.Content,
    attendanceClosed: Boolean,
    onAttend: ((Event) -> Unit)?,
    onRefresh: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = EventIvory) {
        Column(Modifier.fillMaxWidth()) {
            HorizontalDivider(color = EventForest.copy(alpha = 0.18f))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onAttend?.invoke(event) },
                        enabled = onAttend != null && !attendanceClosed && !state.updating && !state.refreshRequired &&
                            (!state.authenticated || event.isAttending != null),
                        modifier = Modifier.weight(1.6f).heightIn(min = 52.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(when {
                            state.updating -> R.string.event_attendance_updating
                            event.isAttending == true -> R.string.event_cancel_attendance
                            else -> R.string.event_attend
                        }))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.event_available_count, event.availableSpots),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (event.capacity > 0) {
                            Text(
                                stringResource(R.string.event_total_capacity, event.capacity),
                                style = MaterialTheme.typography.bodySmall,
                                color = EventSecondary
                            )
                        }
                    }
                }
                if (attendanceClosed) {
                    Text(stringResource(R.string.event_attendance_closed), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                } else if (!state.updating) {
                    when (state.conflict) {
                        AttendanceConflict.AlreadyRegistered -> Text(stringResource(R.string.event_already_registered), style = MaterialTheme.typography.bodySmall)
                        AttendanceConflict.PendingRegistration, AttendanceConflict.Unconfirmed ->
                            Text(stringResource(R.string.event_attendance_conflict), style = MaterialTheme.typography.bodySmall, color = colors.error)
                        else -> if (state.refreshRequired) {
                            val message = if (state.attendanceError) R.string.event_attendance_error else R.string.event_detail_error
                            Text(stringResource(message), style = MaterialTheme.typography.bodySmall, color = colors.error)
                        }
                    }
                    if (state.refreshRequired) {
                        TextButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                    }
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
private fun EventCoverAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = EventForest
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = EventIvory,
            contentColor = tint,
            disabledContainerColor = EventIvory,
            disabledContentColor = tint.copy(alpha = 0.38f)
        )
    ) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EventDescription(event: Event) {
    val description = event.description?.takeIf { it.isNotBlank() } ?: event.summary
    var expanded by rememberSaveable(event.id, description) { mutableStateOf(false) }
    var overflows by remember(event.id, description) { mutableStateOf(false) }
    if (description.isBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.event_description_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EventDetailPreview() {
    EventiaTheme {
        Surface {
            EventDetailContent(
                state = EventDetailUiState.Content(Event(
                    id = 1,
                    title = "Noche de jazz al aire libre con músicos invitados de toda la región",
                    summary = "Un recorrido por los clásicos del jazz con músicos invitados de la región. Trae abrigo para disfrutar de la música al aire libre.",
                    description = null,
                    startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-04:00"),
                    endsAt = null,
                    category = Category(1, "Música"),
                    imageUrl = null,
                    venue = Venue(1, "Casa de la Cultura", "Plaza principal"),
                    capacity = 50,
                    availableSpots = 36,
                    isAttending = false
                )),
                onRetry = {},
                onBack = {},
                onVenueClick = {},
                onAttend = {},
                favoriteAction = { EventCoverAction(favoriteIcon(false), stringResource(favoriteLabel(false)), {}) }
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
