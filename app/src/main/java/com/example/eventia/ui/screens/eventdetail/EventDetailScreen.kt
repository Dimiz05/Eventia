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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.ui.theme.Sage
import com.example.eventia.ui.theme.PaleSage
import com.example.eventia.ui.theme.MutedInk
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.domain.model.hasStarted
import com.example.eventia.ui.components.DetailLocationMap
import com.example.eventia.ui.components.DetailTopBar
import com.example.eventia.ui.components.DetailArtwork
import com.example.eventia.ui.components.DetailSkeleton
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.components.EventDisplayLocale
import com.example.eventia.ui.components.EventDisplayTime
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val EventSurface = Color.White
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
            tint = if (selected) Sage else MutedInk
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
internal fun EventDetailContent(
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
            topBar = { DetailTopBar(onBack) }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (state) {
                    EventDetailUiState.Loading -> DetailSkeleton(stringResource(R.string.event_detail_loading), EventCoverHeight)
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
    val colors = MaterialTheme.colorScheme
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
                        HorizontalDivider(color = Sage.copy(alpha = 0.54f))
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
            ExploreBadge(event.category.name, containerColor = PaleSage, contentColor = Sage)
            Text(
                event.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Sage
            )
        }
        EventDateBadge(event)
    }
}

@Composable
private fun EventDateBadge(event: Event) {
    val locale = EventDisplayLocale
    val start = event.startsAt
    val day = start.format(DateTimeFormatter.ofPattern("d", locale))
    val month = start.format(DateTimeFormatter.ofPattern("MMM", locale)).trimEnd('.').uppercase(locale)
    val time = start.format(EventDisplayTime)
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val monthEnd = with(density) { (typography.headlineLarge.lineHeight.toPx() + typography.labelLarge.lineHeight.toPx() + 4.dp.toPx()) }
    val transition = with(density) { 14.dp.toPx() }
    Column(
        Modifier.widthIn(min = 88.dp).clip(RoundedCornerShape(16.dp)).background(
            Brush.verticalGradient(
                colors = listOf(PaleSage, Sage),
                startY = monthEnd,
                endY = monthEnd + transition
            )
        ).padding(start = 10.dp, top = 4.dp, end = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(day, style = typography.headlineLarge, fontWeight = FontWeight.Bold, color = Sage)
        Text(month, style = typography.labelLarge, fontWeight = FontWeight.Bold, color = Sage)
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
            Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(24.dp), tint = Sage)
            Column(Modifier.weight(1f)) {
                Text(event.venue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (event.venue.address.isNotBlank()) {
                    Text(event.venue.address, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
                }
            }
            Icon(
                painterResource(R.drawable.ic_back), null,
                Modifier.size(16.dp).graphicsLayer { rotationZ = 180f },
                tint = Sage
            )
        }
        DetailLocationMap(event.venue, point, Modifier.fillMaxWidth().height(124.dp).clip(RoundedCornerShape(16.dp)))
        OutlinedButton(
            onClick = openDirections,
            enabled = point != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Sage.copy(alpha = if (point != null) 1f else 0.38f))
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
    val radius = 14.dp
    val panelBorder = colors.outlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EventSurface,
        shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    ) {
        Column(Modifier.fillMaxWidth().drawWithCache {
            val corner = radius.toPx()
            val stroke = 1.dp.toPx()
            val inset = stroke / 2
            val topEdge = Path().apply {
                arcTo(Rect(inset, inset, inset + 2 * corner, inset + 2 * corner), 180f, 90f, true)
                arcTo(Rect(size.width - inset - 2 * corner, inset, size.width - inset, inset + 2 * corner),
                    270f, 90f, false)
            }
            onDrawWithContent {
                drawContent()
                drawPath(topEdge, panelBorder, style = Stroke(stroke))
            }
        }) {
            state.attendanceConfirmation?.let { attending ->
                Text(
                    stringResource(if (attending) R.string.attendance_confirmed else R.string.attendance_canceled),
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp).semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                    color = Sage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
                    EventCapacityTicket(event, Modifier.weight(1f))
                }
                if (attendanceClosed) {
                    Text(stringResource(R.string.event_attendance_closed), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                } else if (!state.updating) {
                    when (state.conflict) {
                        AttendanceConflict.AlreadyRegistered -> Text(stringResource(R.string.event_already_registered), style = MaterialTheme.typography.bodySmall)
                        AttendanceConflict.PendingRegistration, AttendanceConflict.Unconfirmed ->
                            AttendanceErrorNotice(R.string.event_attendance_conflict)
                        else -> if (state.refreshRequired) {
                            val message = if (state.attendanceError) R.string.event_attendance_error else R.string.event_detail_error
                            AttendanceErrorNotice(message)
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
private fun EventCapacityTicket(event: Event, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    var footerHeight by remember { mutableIntStateOf(0) }
    val hasCapacity = event.capacity > 0
    val available = event.availableSpots.toString()
    val label = stringResource(R.string.event_available_count, event.availableSpots)
    val numberStart = label.indexOf(available)
    val emphasis = SpanStyle(fontSize = type.headlineSmall.fontSize)
    val availableText = buildAnnotatedString {
        if (numberStart < 0) {
            append(label)
        } else {
            append(label.substring(0, numberStart))
            withStyle(emphasis) { append(available) }
            append(label.substring(numberStart + available.length))
        }
    }

    Column(
        modifier.drawWithCache {
            val stroke = 1.dp.toPx()
            val inset = stroke / 2
            val notch = 5.dp.toPx()
            val seam = size.height - footerHeight
            val outline = Path().apply {
                addRoundRect(RoundRect(Rect(inset, inset, size.width - inset, size.height - inset),
                    CornerRadius(10.dp.toPx())))
            }
            val perforated = hasCapacity && footerHeight > 0
            val ticket = if (perforated) {
                val cutouts = Path().apply {
                    addOval(Rect(Offset(inset, seam), notch))
                    addOval(Rect(Offset(size.width - inset, seam), notch))
                }
                Path.combine(PathOperation.Difference, outline, cutouts)
            } else outline
            val dashes = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
            onDrawWithContent {
                drawPath(ticket, colors.primaryContainer)
                clipPath(ticket) {
                    if (perforated) {
                        drawLine(colors.outline, Offset(notch, seam), Offset(size.width - notch, seam),
                            strokeWidth = stroke, pathEffect = dashes)
                    }
                    this@onDrawWithContent.drawContent()
                }
                drawPath(ticket, colors.outline, style = Stroke(stroke))
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            availableText,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = type.labelLarge.copy(lineHeight = type.headlineSmall.lineHeight),
            color = colors.onPrimaryContainer,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (hasCapacity) {
            Box(Modifier.fillMaxWidth().onSizeChanged { footerHeight = it.height }, contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.event_total_capacity, event.capacity),
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = type.bodySmall,
                    color = colors.onPrimaryContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AttendanceErrorNotice(message: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer
    ) {
        Text(
            stringResource(message),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )
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
    tint: Color = Sage
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = EventSurface,
            contentColor = tint,
            disabledContainerColor = EventSurface,
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


@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EventLoadingPreview() {
    EventiaTheme {
        EventDetailContent(EventDetailUiState.Loading, onRetry = {}, onBack = {}, onVenueClick = {})
    }
}
