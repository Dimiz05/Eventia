package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.ui.theme.EventiaTheme

@Composable
fun ExploreScreen(viewModel: ExploreViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExploreContent(state, viewModel::retry)
}

@Composable
private fun ExploreContent(state: ExploreUiState, onRetry: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (val current = state) {
            ExploreUiState.Loading -> item { CircularProgressIndicator() }
            is ExploreUiState.Content -> {
                item {
                    Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleLarge)
                }
                items(current.categories, key = { "category-${it.id}" }) { Text(it.name) }
                if (current.categories.isEmpty()) item { Text(stringResource(R.string.categories_empty)) }

                item {
                    Text(stringResource(R.string.events_title), style = MaterialTheme.typography.titleLarge)
                }
                items(current.events, key = { "event-${it.id}" }) { event ->
                    Column {
                        Text(event.title, style = MaterialTheme.typography.titleMedium)
                        Text(event.summary)
                        Text(stringResource(R.string.available_spots, event.availableSpots))
                    }
                }
                if (current.events.isEmpty()) item { Text(stringResource(R.string.events_empty)) }

                item {
                    Text(stringResource(R.string.venues_title), style = MaterialTheme.typography.titleLarge)
                }
                items(current.venues, key = { "venue-${it.id}" }) { venue ->
                    Column {
                        Text(venue.name, style = MaterialTheme.typography.titleMedium)
                        Text(venue.address)
                    }
                }
                if (current.venues.isEmpty()) item { Text(stringResource(R.string.venues_empty)) }
            }
            ExploreUiState.Empty -> item { Text(stringResource(R.string.catalog_empty)) }
            ExploreUiState.Error -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.catalog_error))
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorePreview() {
    EventiaTheme {
        ExploreContent(
            state = ExploreUiState.Content(listOf(Category(1, "Música"), Category(2, "Cultura")), emptyList(), emptyList()),
            onRetry = {}
        )
    }
}
