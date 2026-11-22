package com.example.eventia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onVenueClick: (Long) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    onFavoritesClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showVenues by rememberSaveable { mutableStateOf(false) }
    val sectionState = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize()) {
        ExploreSections(showVenues, onSelect = { showVenues = it })
        sectionState.SaveableStateProvider(showVenues) {
            if (showVenues) {
                VenueListScreen(onVenueClick)
            } else {
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
        }
    }
}

@Composable
private fun ExploreSections(showVenues: Boolean, onSelect: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        SegmentedButton(
            selected = !showVenues,
            onClick = { onSelect(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) { Text(stringResource(R.string.events_title)) }
        SegmentedButton(
            selected = showVenues,
            onClick = { onSelect(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) { Text(stringResource(R.string.venues_title)) }
    }
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
        OutlinedTextField(
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(painterResource(R.drawable.ic_explore), null) },
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
            if (filters.isActive && (events?.isEmpty() != true || state.hasError)) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.catalog_clear_filters))
                }
            }
        }
        if (state.isLoading) {
            val loadingLabel = stringResource(R.string.catalog_loading)
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    .semantics { contentDescription = loadingLabel }
            )
        }
        if (state.hasError && events != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.catalog_refresh_error))
                    Button(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                }
            }
        }
        when {
            state.isLoading && events.isNullOrEmpty() -> CatalogSkeleton(Modifier.weight(1f))
            state.hasError && events == null -> {
                Column(Modifier.weight(1f)) {
                    ScreenMessage(
                        title = stringResource(R.string.catalog_error),
                        modifier = Modifier.weight(1f),
                        description = stringResource(R.string.catalog_error_recovery),
                        isError = true,
                        actionLabel = stringResource(R.string.retry),
                        onAction = onRefresh
                    )
                    if (onFavoritesClick != null) {
                        TextButton(onClick = onFavoritesClick) {
                            Text(stringResource(R.string.tab_favorites))
                        }
                    }
                }
            }
            events != null && events.isEmpty() && !state.hasError -> {
                ScreenMessage(
                    title = stringResource(
                        when {
                            filters.isActive -> R.string.catalog_no_matches_title
                            content?.categories.isNullOrEmpty() -> R.string.catalog_empty
                            else -> R.string.events_empty
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    description = if (filters.isActive) stringResource(R.string.catalog_no_matches) else null,
                    actionLabel = if (filters.isActive) stringResource(R.string.catalog_clear_filters) else null,
                    onAction = if (filters.isActive) onClearFilters else null
                )
            }
            !events.isNullOrEmpty() -> key(filters) {
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(events, key = { it.id }) { event -> EventCard(event, onEventClick) }
                }
            }
        }
    }
}

@Composable
private fun CatalogSkeleton(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.surfaceContainerHighest
    val shape = RoundedCornerShape(8.dp)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(88.dp).background(tint, shape))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(0.35f).height(12.dp).background(tint, shape))
                    Box(Modifier.fillMaxWidth(0.9f).height(18.dp).background(tint, shape))
                    Box(Modifier.fillMaxWidth(0.6f).height(12.dp).background(tint, shape))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorePreview() {
    ExploreStatePreview(
        ExploreUiState(
            filters = ExploreFilters(query = "Jazz"),
            content = ExploreUiState.Content(
                listOf(Category(1, "Música"), Category(2, "Cultura")),
                emptyList()
            ),
            isLoading = false
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun ExploreLoadingPreview() {
    ExploreStatePreview(ExploreUiState())
}

@Preview(showBackground = true)
@Composable
private fun ExploreErrorPreview() {
    ExploreStatePreview(ExploreUiState(isLoading = false, hasError = true))
}

@Composable
private fun ExploreStatePreview(state: ExploreUiState) {
    EventiaTheme {
        Surface {
            Column {
                ExploreSections(showVenues = false, onSelect = {})
                ExploreContent(state, {}, {}, {}, {}, {})
            }
        }
    }
}
