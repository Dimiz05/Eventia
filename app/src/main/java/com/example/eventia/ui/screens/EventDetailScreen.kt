package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun EventDetailScreen(
    onBack: () -> Unit,
    onVenueClick: (Long) -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EventDetailContent(state, viewModel::retry, onBack, onVenueClick)
}

@Composable
private fun EventDetailContent(
    state: EventDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onVenueClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            EventDetailUiState.Loading -> CircularProgressIndicator()
            EventDetailUiState.Empty -> {
                Text(stringResource(R.string.event_detail_empty))
                Button(onClick = onBack) { Text(stringResource(R.string.navigate_back)) }
            }
            EventDetailUiState.Error -> {
                Text(stringResource(R.string.event_detail_error))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            is EventDetailUiState.Content -> {
                val event = state.event
                Text(event.title, style = MaterialTheme.typography.headlineSmall)
                Text(event.category.name, style = MaterialTheme.typography.labelLarge)
                EventArtwork(event.imageUrl)
                Text(eventLocalDateTime(event))
                Text(stringResource(R.string.available_spots, event.availableSpots))
                Column {
                    TextButton(onClick = { onVenueClick(event.venue.id) }) {
                        Text(event.venue.name, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(event.venue.address)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {}, enabled = false) {
                            Text(stringResource(R.string.event_directions))
                        }
                        Button(onClick = {}, enabled = false) {
                            Text(stringResource(R.string.event_attend))
                        }
                    }
                }
                Text(stringResource(R.string.event_description_title), style = MaterialTheme.typography.titleMedium)
                Text(event.description?.takeIf { it.isNotBlank() } ?: event.summary)
            }
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
