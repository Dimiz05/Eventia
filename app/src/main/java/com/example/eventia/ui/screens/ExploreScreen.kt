package com.example.eventia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.ui.theme.EventiaTheme

@Composable
fun ExploreScreen(
    onEventClick: (Long) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    onFavoritesClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExploreContent(
        state = state,
        onQueryChange = viewModel::changeQuery,
        onCategorySelect = viewModel::selectCategory,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::refresh,
        onEventClick = onEventClick,
        onFavoritesClick = onFavoritesClick
    )
}

@Composable
private fun ExploreContent(
    state: ExploreUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onEventClick: (Long) -> Unit,
    onFavoritesClick: (() -> Unit)? = null
) {
    val filters = state.filters
    val content = state.content
    val events = content?.events
    Column(Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = true,
                onClick = {},
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.events_title)) }
            SegmentedButton(
                selected = false,
                onClick = {},
                enabled = false,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.venues_title)) }
        }
        OutlinedTextField(
            value = filters.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.catalog_search)) },
            singleLine = true
        )
        if (content != null && content.categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(content.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = filters.categoryId == category.id,
                        onClick = { onCategorySelect(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }
        }
        Row(Modifier.padding(horizontal = 24.dp)) {
            TextButton(onClick = onRefresh, enabled = !state.isLoading && !state.hasError) {
                Text(stringResource(R.string.catalog_refresh))
            }
            if (filters.query.isNotEmpty() || filters.categoryId != null) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.catalog_clear_filters))
                }
            }
        }
        if (state.isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (state.hasError) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                val message = if (events == null) R.string.catalog_error else R.string.catalog_refresh_error
                Text(stringResource(message))
                Button(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                if (events == null && onFavoritesClick != null) {
                    TextButton(onClick = onFavoritesClick) { Text(stringResource(R.string.tab_favorites)) }
                }
            }
        }
        key(filters) {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (content == null || events == null) return@LazyColumn

                if (filters.isActive && events.isEmpty()) {
                    item { Text(stringResource(R.string.catalog_no_matches)) }
                    return@LazyColumn
                }
                if (events.isEmpty()) {
                    val message = if (content.categories.isEmpty()) R.string.catalog_empty else R.string.events_empty
                    item { Text(stringResource(message)) }
                    return@LazyColumn
                }

                items(events, key = { "event-${it.id}" }) { event -> EventCard(event, onEventClick) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorePreview() {
    EventiaTheme {
        ExploreContent(
            state = ExploreUiState(
                content = ExploreUiState.Content(listOf(Category(1, "Música"), Category(2, "Cultura")), emptyList(), emptyList()),
                isLoading = false
            ),
            onQueryChange = {},
            onCategorySelect = {},
            onClearFilters = {},
            onRefresh = {},
            onEventClick = {}
        )
    }
}
