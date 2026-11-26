package com.example.eventia.ui.screens.venue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.screens.explore.ExploreBadge
import com.example.eventia.ui.screens.explore.ExploreCard
import com.example.eventia.ui.screens.explore.ExploreMetadata
import com.example.eventia.ui.screens.explore.ExplorePreviewFrame
import com.example.eventia.ui.screens.explore.ExplorePullToRefresh
import com.example.eventia.ui.screens.explore.ExploreSearch
import com.example.eventia.ui.screens.explore.ExploreSections

@Composable
internal fun VenueListScreen(
    onVenueClick: (Long) -> Unit,
    sections: @Composable () -> Unit,
    viewModel: VenueListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VenueListContent(state, viewModel::changeQuery, viewModel::refresh, onVenueClick, sections)
}

@Composable
internal fun VenueListContent(
    state: VenueListUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onVenueClick: (Long) -> Unit,
    sections: @Composable () -> Unit
) {
    val venues = state.venues
    val matches = state.matches
    ExplorePullToRefresh(
        onRefresh = onRefresh,
        isLoading = state.isLoading,
        hasScrollableContent = matches.isNotEmpty()
    ) {
        ExploreSearch(
            state.query,
            onQueryChange,
            stringResource(R.string.venue_search)
        )
        sections()
        if (venues != null) {
            if (state.hasError) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.venue_list_refresh_error))
                    TextButton(onClick = onRefresh, enabled = !state.isLoading) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }
            if (venues.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.venue_list_heading),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val colors = MaterialTheme.colorScheme
                    ExploreBadge(
                        stringResource(R.string.venue_list_count, matches.size),
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    )
                }
            }
        }
        when {
            venues == null && state.isLoading -> LoadingScreen(stringResource(R.string.venue_list_loading))
            venues == null -> ScreenMessage(
                title = stringResource(R.string.venue_list_error),
                isError = true,
                actionLabel = stringResource(R.string.retry),
                onAction = onRefresh
            )
            venues.isEmpty() -> ScreenMessage(title = stringResource(R.string.venue_list_empty))
            matches.isEmpty() -> ScreenMessage(
                title = stringResource(R.string.venue_no_matches),
                actionLabel = stringResource(R.string.venue_clear_search),
                onAction = { onQueryChange("") }
            )
            else -> key(state.query.trim()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(matches, key = { it.id }) { VenueCard(it, onVenueClick) }
                }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, onVenueClick: (Long) -> Unit) {
    val count = venue.upcomingEventsCount
    ExploreCard(
        imageUrl = venue.imageUrl,
        fallback = R.drawable.ic_catalog_buildings,
        onClick = { onVenueClick(venue.id) },
        cover = {
            if (count != null && count >= 0) {
                ExploreBadge(
                    pluralStringResource(R.plurals.venue_upcoming_event_count, count, count),
                    Modifier.align(Alignment.TopStart).padding(12.dp),
                    icon = R.drawable.ic_catalog_ticket
                )
            }
        }
    ) {
        Text(venue.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (venue.address.isNotBlank()) ExploreMetadata(R.drawable.ic_catalog_pin, venue.address)
        val hours = venue.openingHours
        if (!hours.isNullOrBlank()) ExploreMetadata(R.drawable.ic_catalog_clock, hours)
        if (count == null || count < 0) {
            Text(
                stringResource(R.string.venue_event_count_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VenueListPreview() {
    ExplorePreviewFrame {
        VenueListContent(
            VenueListUiState(
                venues = listOf(
                    Venue(1, "Casa de la Cultura", "Av. Principal 123, Casco Histórico", openingHours = "09:00–22:00", upcomingEventsCount = 3),
                    Venue(2, "Cine Teatro Municipal", "Calle Sucre 450, Centro", openingHours = "15:00–23:00", upcomingEventsCount = 2)
                )
            ),
            {}, {}, {},
            sections = { ExploreSections(showVenues = true, onSelect = {}) }
        )
    }
}
