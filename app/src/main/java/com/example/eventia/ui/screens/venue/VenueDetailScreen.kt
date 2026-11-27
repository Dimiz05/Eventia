package com.example.eventia.ui.screens.venue

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.DetailArtwork
import com.example.eventia.ui.components.DetailLocationMap
import com.example.eventia.ui.components.DetailTopBar
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.rememberVenueDirections
import com.example.eventia.ui.screens.eventdetail.venueMapUrl
import com.example.eventia.ui.screens.explore.ExploreBadge
import com.example.eventia.ui.screens.explore.ExploreEventCard
import com.example.eventia.ui.screens.explore.ExploreFavoriteButton
import com.example.eventia.ui.screens.explore.exploreColors
import com.example.eventia.ui.screens.favorites.FavoritesUiState
import com.example.eventia.ui.screens.map.MapPoint
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime

@Composable
internal fun VenueDetailScreen(
    onBack: () -> Unit,
    onEventClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit,
    viewModel: VenueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VenueDetailContent(state, onBack, viewModel::retry, onEventClick, favorites, onFavoriteChange)
}

@Composable
private fun VenueDetailContent(
    state: VenueDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEventClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit
) {
    MaterialTheme(colorScheme = exploreColors()) {
        Scaffold(
            topBar = {
                if (state !is VenueDetailUiState.Content) {
                    DetailTopBar(stringResource(R.string.detail_venue), onBack)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            when (state) {
                VenueDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                    LoadingScreen(stringResource(R.string.venue_detail_loading))
                }
                VenueDetailUiState.Empty -> Box(Modifier.fillMaxSize().padding(padding)) {
                    ScreenMessage(
                        title = stringResource(R.string.venue_detail_not_found),
                        actionLabel = stringResource(R.string.navigate_back),
                        onAction = onBack
                    )
                }
                VenueDetailUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                    ScreenMessage(
                        title = stringResource(R.string.venue_detail_error),
                        isError = true,
                        actionLabel = stringResource(R.string.retry),
                        onAction = onRetry
                    )
                }
                is VenueDetailUiState.Content -> VenueDetailBody(
                    state = state,
                    modifier = Modifier.padding(padding),
                    onBack = onBack,
                    onRetry = onRetry,
                    onEventClick = onEventClick,
                    favorites = favorites,
                    onFavoriteChange = onFavoriteChange
                )
            }
        }
    }
}

@Composable
private fun VenueDetailBody(
    state: VenueDetailUiState.Content,
    modifier: Modifier,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEventClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit
) {
    val venue = state.venue
    val colors = MaterialTheme.colorScheme
    val loadedEvents = state.events as? VenueEventsUiState.Loaded
    val count = (loadedEvents?.events?.size ?: venue.upcomingEventsCount)?.takeIf { it >= 0 }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.surface),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box {
                VenueCover(venue, onBack)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 152.dp),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                ) {
                    Column(
                        Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        VenueHeading(venue, count)
                        VenueDivider()
                        venue.description?.takeIf(String::isNotBlank)?.let { description ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                VenueSectionTitle(stringResource(R.string.venue_about))
                                Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                            VenueDivider()
                        }
                        VenueLocation(venue)
                        VenueDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VenueSectionTitle(
                                stringResource(R.string.venue_detail_upcoming_events),
                                Modifier.weight(1f)
                            )
                            if (count != null) VenueBadge(stringResource(R.string.venue_list_count, count))
                        }
                    }
                }
            }
        }
        when (val events = state.events) {
            VenueEventsUiState.Loading -> item { LoadingScreen(stringResource(R.string.venue_events_loading)) }
            VenueEventsUiState.Error -> item {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Text(stringResource(R.string.venue_events_error), color = colors.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
            is VenueEventsUiState.Loaded -> {
                if (events.events.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.venue_events_empty),
                            Modifier.padding(horizontal = 18.dp),
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                items(events.events, key = { it.id }) { event ->
                    Box(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 12.dp)) {
                        ExploreEventCard(
                            event = event,
                            onEventClick = onEventClick,
                            favorite = {
                                val selected = event.id in favorites.ids
                                ExploreFavoriteButton(selected, favorites.canChange(event.id)) {
                                    onFavoriteChange(event, !selected)
                                }
                            },
                            height = 156.dp,
                            multiline = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VenueCover(venue: Venue, onBack: () -> Unit) {
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.event_share)
    val shareText = stringResource(R.string.venue_share_text, venue.name, venue.address, venueMapUrl(venue))
    Box(Modifier.fillMaxWidth().height(176.dp)) {
        DetailArtwork(venue.imageUrl, R.drawable.ic_catalog_buildings, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxWidth().height(88.dp).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent))
            )
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            VenueCoverAction(R.drawable.ic_back, stringResource(R.string.navigate_back), onBack)
            VenueCoverAction(R.drawable.ic_event_share, shareLabel) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, shareLabel))
            }
        }
    }
}

@Composable
private fun VenueCoverAction(icon: Int, label: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(painterResource(icon), label, Modifier.size(22.dp))
    }
}

@Composable
private fun VenueHeading(venue: Venue, count: Int?) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        if (maxWidth < 290.dp || fontScale > 1.25f) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VenueIdentity(venue, count)
                VenueHours(venue)
            }
        } else {
            val hoursMaxWidth = maxWidth * 0.37f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VenueIdentity(venue, count, Modifier.weight(1f))
                VenueHours(venue, Modifier.widthIn(max = hoursMaxWidth))
            }
        }
    }
}

@Composable
private fun VenueIdentity(venue: Venue, count: Int?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (count != null) {
            VenueBadge(
                pluralStringResource(R.plurals.venue_upcoming_event_count, count, count),
                R.drawable.ic_catalog_ticket
            )
        }
        Text(venue.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (venue.address.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(18.dp))
                Text(venue.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VenueHours(venue: Venue, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = colors.primaryContainer) {
        Row(
            Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_catalog_clock), null, Modifier.size(20.dp))
            Column {
                Text(stringResource(R.string.venue_hours), style = MaterialTheme.typography.labelMedium)
                Text(
                    venue.openingHours?.takeIf(String::isNotBlank)
                        ?: stringResource(R.string.venue_hours_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VenueLocation(venue: Venue) {
    val colors = MaterialTheme.colorScheme
    val point = MapPoint.from(venue.latitude, venue.longitude)
    val directions = rememberVenueDirections(venue, point)
    val address = when {
        venue.address.isNotBlank() -> venue.address
        point != null -> stringResource(
            R.string.venue_location_coordinates,
            point.latitude.toString(),
            point.longitude.toString()
        )
        else -> stringResource(R.string.venue_address_unavailable)
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(24.dp))
            Column {
                VenueSectionTitle(stringResource(R.string.venue_location))
                Text(address, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        DetailLocationMap(venue, point, Modifier.fillMaxWidth().height(126.dp).clip(RoundedCornerShape(16.dp)))
        OutlinedButton(
            onClick = directions,
            enabled = point != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = if (point != null) 1f else 0.38f))
        ) {
            Icon(painterResource(R.drawable.ic_map), null, Modifier.size(19.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.venue_directions), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VenueSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun VenueBadge(label: String, icon: Int? = null) {
    val colors = MaterialTheme.colorScheme
    ExploreBadge(label, icon = icon, containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer)
}

@Composable
private fun VenueDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f))
}

@Preview(showBackground = true)
@Composable
private fun VenueDetailPreview() {
    val venue = Venue(
        id = 7,
        name = "Centro Cultural Estación del Norte",
        address = "Av. de las Artes 450, Distrito Histórico",
        description = "Un espacio arquitectónico dedicado a la música, las artes y encuentros culturales.",
        openingHours = "09:00 – 22:00",
        imageUrl = null,
        latitude = -12.0464,
        longitude = -77.0428,
        upcomingEventsCount = 3
    )
    val event = Event(
        id = 19,
        title = "Gala Sinfónica de Primavera",
        summary = "Repertorio magistral con solistas invitados de la Orquesta Regional.",
        description = null,
        startsAt = OffsetDateTime.parse("2030-11-14T20:00:00-05:00"),
        endsAt = null,
        category = Category(2, "Música Clásica"),
        imageUrl = null,
        venue = venue,
        capacity = 120,
        availableSpots = 36,
        isAttending = null
    )
    val upcoming = listOf(
        event,
        event.copy(
            id = 20,
            title = "La Sombra del Espejo",
            summary = "Una obra de cámara sobre identidad y memoria.",
            category = Category(3, "Teatro"),
            availableSpots = 18,
            startsAt = event.startsAt.plusDays(8)
        ),
        event.copy(
            id = 21,
            title = "Encuentro de jazz contemporáneo",
            summary = "Composiciones originales y ritmos latinoamericanos en vivo.",
            category = Category(4, "Jazz"),
            availableSpots = 42,
            startsAt = event.startsAt.plusDays(16)
        )
    )
    CompositionLocalProvider(LocalInspectionMode provides true) {
        EventiaTheme {
            VenueDetailContent(
                VenueDetailUiState.Content(venue, VenueEventsUiState.Loaded(upcoming)),
                {}, {}, {}, FavoritesUiState(events = emptyList()), { _, _ -> }
            )
        }
    }
}
