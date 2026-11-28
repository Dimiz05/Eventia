package com.example.eventia.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.ui.components.TabHeader
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.screens.explore.ExploreBadge
import com.example.eventia.ui.screens.explore.ExploreEventCard
import com.example.eventia.ui.screens.explore.ExploreFavoriteButton
import com.example.eventia.ui.screens.explore.exploreColors
import com.example.eventia.ui.theme.EventiaTheme
import java.time.OffsetDateTime

@Composable
internal fun FavoritesScreen(
    state: FavoritesUiState,
    onEventClick: (Long) -> Unit,
    onRemove: (Event) -> Unit,
    onRetry: () -> Unit,
    onExplore: () -> Unit,
    avatar: @Composable () -> Unit = {}
) {
    val events = state.events
    MaterialTheme(colorScheme = exploreColors()) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                TabHeader(stringResource(R.string.tab_favorites), avatar)
                when {
                    state.hasError -> ScreenMessage(
                        title = stringResource(R.string.favorites_load_error),
                        isError = true,
                        actionLabel = stringResource(R.string.retry),
                        onAction = onRetry
                    )
                    events == null -> LoadingScreen(stringResource(R.string.favorites_loading))
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (events.isNotEmpty()) item { FavoritesStorageBanner(events.size) }
                        if (events.isEmpty()) {
                            item { EmptyFavorites(onExplore) }
                        }
                        items(events, key = Event::id) { event ->
                            SavedEventCard(
                                event,
                                canRemove = state.canChange(event.id),
                                onOpen = { onEventClick(event.id) },
                                onRemove = { onRemove(event) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesStorageBanner(count: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary) {
                Icon(painterResource(R.drawable.ic_session_check), null, Modifier.padding(3.dp).size(16.dp))
            }
            Text(
                stringResource(R.string.favorites_saved_data),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium
            )
            ExploreBadge(
                pluralStringResource(R.plurals.favorites_saved_count, count, count),
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                containerColor = colors.surface,
                contentColor = colors.primary
            )
        }
    }
}

@Composable
private fun EmptyFavorites(onExplore: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                Icon(painterResource(R.drawable.ic_favorite_outline), null, Modifier.padding(15.dp).size(28.dp))
            }
            Text(
                stringResource(R.string.favorites_empty),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.favorites_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant
            )
            Button(onClick = onExplore) { Text(stringResource(R.string.favorites_explore)) }
        }
    }
}

@Composable
private fun SavedEventCard(event: Event, canRemove: Boolean, onOpen: () -> Unit, onRemove: () -> Unit) {
    ExploreEventCard(
        event = event,
        onEventClick = { onOpen() },
        favorite = { ExploreFavoriteButton(selected = true, enabled = canRemove, onClick = onRemove) },
        height = 164.dp,
        multiline = true
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 850)
@Preview(showBackground = true, widthDp = 390, heightDp = 850, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FavoritesPreview() {
    val jazz = Event(
        id = 1,
        title = "Noche de jazz en la plaza",
        summary = "",
        description = null,
        startsAt = OffsetDateTime.parse("2030-11-14T20:30:00-04:00"),
        endsAt = null,
        category = Category(1, "Música"),
        imageUrl = null,
        venue = Venue(1, "Plaza del Carmen, Centro", "Calle Sucre"),
        capacity = 50,
        availableSpots = 36,
        isAttending = null
    )
    val saved = listOf(
        jazz,
        jazz.copy(
            id = 2,
            title = "Taller de cerámica contemporánea",
            startsAt = jazz.startsAt.plusDays(1),
            category = Category(2, "Arte y talleres"),
            venue = Venue(2, "Estudio Barro Vivo", "Barrio Alto")
        ),
        jazz.copy(id = 3, title = "Feria de diseño local", category = Category(3, "Diseño"))
    )
    FavoritesPreviewState(FavoritesUiState(events = saved, ids = saved.map(Event::id).toSet()))
}

@Preview(showBackground = true)
@Composable
private fun FavoritesEmptyPreview() {
    FavoritesPreviewState(FavoritesUiState(events = emptyList()))
}

@Composable
private fun FavoritesPreviewState(state: FavoritesUiState) {
    EventiaTheme {
        val colors = exploreColors()
        Box(Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding()) {
            FavoritesScreen(state, {}, {}, {}, {})
        }
    }
}
