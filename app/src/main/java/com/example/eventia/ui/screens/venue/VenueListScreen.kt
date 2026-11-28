package com.example.eventia.ui.screens.venue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.screens.explore.ExploreBadge
import com.example.eventia.ui.screens.explore.ExplorePreviewFrame
import com.example.eventia.ui.screens.explore.ExplorePullToRefresh
import com.example.eventia.ui.screens.explore.ExploreSearch
import com.example.eventia.ui.screens.explore.ExploreSections
import com.example.eventia.ui.screens.explore.panoramicCaptionScrim

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
            stringResource(R.string.venue_search),
            inlineHint = true
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
                val listState = rememberLazyListState()
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val spacing = 12.dp
                    val density = LocalDensity.current
                    val pairHeight = with(density) {
                        ((constraints.maxHeight - 3 * spacing.roundToPx()) / 2).toDp()
                    }
                    val cardHeight = pairHeight.coerceAtLeast(with(density) { 48.dp + 76.sp.toDp() })
                    val multiline = cardHeight >= with(density) { 48.dp + 124.sp.toDp() }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(matches, key = { it.id }) { venue ->
                            VenueCard(venue, onVenueClick, cardHeight, multiline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, onVenueClick: (Long) -> Unit, height: Dp, multiline: Boolean) {
    val count = venue.upcomingEventsCount
    val fallback = painterResource(R.drawable.ic_catalog_buildings)
    val countLabel = if (count != null && count >= 0) {
        pluralStringResource(R.plurals.venue_upcoming_event_count, count, count)
    } else {
        stringResource(R.string.venue_event_count_unavailable)
    }
    Surface(
        onClick = { onVenueClick(venue.id) },
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = venue.imageUrl?.takeIf { it.isNotBlank() },
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                placeholder = fallback,
                fallback = fallback,
                error = fallback
            )
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.76f),
                        0.42f to Color.Transparent,
                        0.52f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.9f)
                    )
                )
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    countLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().panoramicCaptionScrim().padding(16.dp)
            ) {
                Text(
                    venue.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    maxLines = if (multiline) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (venue.address.isNotBlank()) {
                    Text(
                        venue.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFFFFF),
                        maxLines = if (multiline) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val hours = venue.openingHours
                if (!hours.isNullOrBlank()) {
                    Text(
                        hours,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
