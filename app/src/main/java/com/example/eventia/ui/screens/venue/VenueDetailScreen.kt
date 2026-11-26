package com.example.eventia.ui.screens.venue

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
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
import com.example.eventia.ui.components.DetailArtwork
import com.example.eventia.ui.components.DetailLocationMap
import com.example.eventia.ui.components.DetailTopBar
import com.example.eventia.ui.components.FavoriteButton
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.components.rememberVenueDirections
import com.example.eventia.ui.screens.eventdetail.venueMapUrl
import com.example.eventia.ui.screens.explore.ExploreBadge
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
            topBar = { DetailTopBar(stringResource(R.string.detail_venue), onBack) },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                    modifier = Modifier.fillMaxSize().padding(padding),
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
    onRetry: () -> Unit,
    onEventClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit
) {
    val venue = state.venue
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val point = MapPoint.from(venue.latitude, venue.longitude)
    val directions = rememberVenueDirections(venue, point)
    val shareLabel = stringResource(R.string.event_share)
    val shareText = stringResource(R.string.venue_share_text, venue.name, venue.address, venueMapUrl(venue))

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(236.dp)) {
                DetailArtwork(venue.imageUrl, R.drawable.ic_catalog_buildings, Modifier.fillMaxSize())
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)))
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(shape = RoundedCornerShape(50), color = colors.surface.copy(alpha = 0.95f)) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, shareLabel))
                        }) {
                            Icon(painterResource(R.drawable.ic_event_share), shareLabel, tint = colors.onSurface)
                        }
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    venue.upcomingEventsCount?.takeIf { it >= 0 }?.let { count ->
                        ExploreBadge(
                            label = pluralStringResource(R.plurals.venue_upcoming_event_count, count, count),
                            icon = R.drawable.ic_catalog_ticket,
                            containerColor = colors.surface,
                            contentColor = colors.onSurface
                        )
                    }
                    Text(
                        venue.name,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (venue.address.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(17.dp), tint = Color.White)
                            Text(venue.address, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = directions,
                enabled = point != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 52.dp)
            ) {
                Icon(painterResource(R.drawable.ic_map), null, Modifier.size(19.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.venue_directions))
            }
        }
        item {
            DetailSectionCard(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.primaryContainer) {
                        Icon(
                            painterResource(R.drawable.ic_catalog_clock), null,
                            Modifier.padding(12.dp).size(22.dp), tint = colors.onPrimaryContainer
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.venue_hours), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                        Text(
                            venue.openingHours?.takeIf(String::isNotBlank) ?: stringResource(R.string.venue_hours_unavailable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        venue.description?.takeIf(String::isNotBlank)?.let { about ->
            item {
                DetailSectionCard(Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.venue_about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(about, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        item {
            DetailSectionCard(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_map), null, Modifier.size(19.dp), tint = colors.primary)
                    Text(stringResource(R.string.venue_location), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                DetailLocationMap(venue = venue, point = point, modifier = Modifier.fillMaxWidth().height(176.dp))
                val address = when {
                    venue.address.isNotBlank() -> venue.address
                    point != null -> stringResource(
                        R.string.venue_location_coordinates,
                        point.latitude.toString(),
                        point.longitude.toString()
                    )
                    else -> stringResource(R.string.venue_address_unavailable)
                }
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_catalog_pin), null, Modifier.size(17.dp), tint = colors.primary)
                    Text(address, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.venue_detail_upcoming_events),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val loadedEvents = state.events as? VenueEventsUiState.Loaded
                val count = loadedEvents?.events?.size ?: venue.upcomingEventsCount
                if (count != null && count >= 0) {
                    ExploreBadge(
                        stringResource(R.string.venue_list_count, count),
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )
                }
            }
        }
        when (val events = state.events) {
            VenueEventsUiState.Loading -> item { LoadingScreen(stringResource(R.string.venue_events_loading)) }
            VenueEventsUiState.Error -> item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.venue_events_error), color = colors.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
            is VenueEventsUiState.Loaded -> {
                if (events.events.isEmpty()) {
                    item { Text(stringResource(R.string.venue_events_empty), Modifier.padding(horizontal = 16.dp), color = colors.onSurfaceVariant) }
                }
                items(events.events, key = { it.id }) { event ->
                    val selected = event.id in favorites.ids
                    VenueEventCard(
                        event = event,
                        onClick = { onEventClick(event.id) },
                        selected = selected,
                        favoriteEnabled = favorites.canChange(event.id),
                        onFavoriteChange = { onFavoriteChange(event, !selected) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
private fun VenueEventCard(
    event: Event,
    onClick: () -> Unit,
    selected: Boolean,
    favoriteEnabled: Boolean,
    onFavoriteChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                DetailArtwork(event.imageUrl, R.drawable.ic_catalog_ticket, Modifier.fillMaxSize())
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent))
                    )
                )
                ExploreBadge(
                    event.category.name,
                    Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp, end = 64.dp),
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(50),
                    color = colors.surface.copy(alpha = 0.96f)
                ) {
                    FavoriteButton(selected, favoriteEnabled, onFavoriteChange)
                }
            }
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    event.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_event_calendar), null, Modifier.size(17.dp), tint = colors.primary)
                        Text(eventLocalDateTime(event), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    ExploreBadge(
                        label = stringResource(R.string.available_spots, event.availableSpots),
                        containerColor = colors.surfaceContainer,
                        contentColor = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
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
