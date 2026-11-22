package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun VenueListScreen(
    onVenueClick: (Long) -> Unit,
    viewModel: VenueListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VenueListContent(state, viewModel::changeQuery, viewModel::refresh, onVenueClick)
}

@Composable
private fun VenueListContent(
    state: VenueListUiState,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onVenueClick: (Long) -> Unit
) {
    val venues = state.venues
    val matches = state.matches
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(painterResource(R.drawable.ic_explore), null) },
            label = { Text(stringResource(R.string.venue_search)) },
            singleLine = true
        )
        if (venues != null) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                if (state.hasError) Text(stringResource(R.string.venue_list_refresh_error))
                TextButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Text(stringResource(if (state.hasError) R.string.retry else R.string.catalog_refresh))
                }
            }
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 24.dp))
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
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(matches, key = { it.id }) { VenueCard(it, onVenueClick) }
                }
            }
        }
    }
}

@Composable
private fun VenueCard(venue: Venue, onVenueClick: (Long) -> Unit) {
    OutlinedCard(
        onClick = { onVenueClick(venue.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EventArtwork(venue.imageUrl, Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(venue.name, style = MaterialTheme.typography.titleMedium)
                Text(venue.address, style = MaterialTheme.typography.bodyMedium)
                val count = venue.upcomingEventsCount
                Text(
                    if (count == null) stringResource(R.string.venue_event_count_unavailable)
                    else pluralStringResource(R.plurals.venue_upcoming_event_count, count, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VenueListEmptyPreview() {
    EventiaTheme {
        VenueListContent(VenueListUiState(venues = emptyList()), {}, {}, {})
    }
}
