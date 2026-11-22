package com.example.eventia.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun VenueDetailScreen(
    onBack: () -> Unit,
    onEventClick: (Long) -> Unit,
    viewModel: VenueDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VenueDetailContent(state, onBack, viewModel::retry, onEventClick)
}

@Composable
private fun VenueDetailContent(
    state: VenueDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEventClick: (Long) -> Unit
) {
    when (state) {
        VenueDetailUiState.Loading -> LoadingScreen(stringResource(R.string.venue_detail_loading))
        VenueDetailUiState.Empty -> ScreenMessage(
            title = stringResource(R.string.venue_detail_not_found),
            actionLabel = stringResource(R.string.navigate_back),
            onAction = onBack
        )
        VenueDetailUiState.Error -> ScreenMessage(
            title = stringResource(R.string.venue_detail_error),
            isError = true,
            actionLabel = stringResource(R.string.retry),
            onAction = onRetry
        )
        is VenueDetailUiState.Content -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { VenueInformation(state.venue) }
            item {
                Text(
                    stringResource(R.string.venue_upcoming_events),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            when (val events = state.events) {
                VenueEventsUiState.Loading -> item {
                    LoadingScreen(stringResource(R.string.venue_events_loading))
                }
                VenueEventsUiState.Error -> item {
                    Column {
                        Text(stringResource(R.string.venue_events_error))
                        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    }
                }
                is VenueEventsUiState.Loaded -> {
                    if (events.events.isEmpty()) {
                        item { Text(stringResource(R.string.venue_events_empty)) }
                    }
                    items(events.events, key = { it.id }) { EventCard(it, onEventClick) }
                }
            }
        }
    }
}

@Composable
private fun VenueInformation(venue: Venue) {
    val context = LocalContext.current
    val latitude = venue.latitude?.takeIf { it in -90.0..90.0 }
    val longitude = venue.longitude?.takeIf { it in -180.0..180.0 }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EventArtwork(venue.imageUrl)
        Text(venue.name, style = MaterialTheme.typography.headlineSmall)
        venue.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
        Text(stringResource(R.string.venue_hours), style = MaterialTheme.typography.titleMedium)
        Text(venue.openingHours?.takeIf { it.isNotBlank() } ?: stringResource(R.string.venue_hours_unavailable))
        Text(stringResource(R.string.venue_location), style = MaterialTheme.typography.titleMedium)
        Text(venue.address.takeIf { it.isNotBlank() } ?: stringResource(R.string.venue_address_unavailable))
        if (latitude != null && longitude != null) {
            TextButton(onClick = {
                val point = "$latitude,$longitude"
                val query = Uri.encode("$point(${venue.name})")
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$point?q=$query")))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.venue_directions_unavailable, Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(stringResource(R.string.venue_directions))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VenueDetailErrorPreview() {
    EventiaTheme { VenueDetailContent(VenueDetailUiState.Error, {}, {}, {}) }
}
