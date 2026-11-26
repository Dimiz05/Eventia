package com.example.eventia.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.components.CachedDataNotice
import com.example.eventia.ui.components.FavoriteButton
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.cachedCatalogNotice
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.favorites.FavoritesUiState
import com.example.eventia.ui.screens.venue.VenueListScreen
import java.time.OffsetDateTime

@Composable
internal fun ExploreScreen(
    onEventClick: (Long) -> Unit,
    onVenueClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    onFavoritesClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showVenues by rememberSaveable { mutableStateOf(false) }
    val sectionState = rememberSaveableStateHolder()
    val sections: @Composable () -> Unit = { ExploreSections(showVenues) { showVenues = it } }
    MaterialTheme(colorScheme = exploreColors()) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                ExploreHeader()
                sectionState.SaveableStateProvider(showVenues) {
                    if (showVenues) {
                        VenueListScreen(onVenueClick, sections)
                    } else {
                        ExploreContent(
                            state = state,
                            onQueryChange = viewModel::changeQuery,
                            onCategorySelect = viewModel::selectCategory,
                            onClearFilters = viewModel::clearFilters,
                            onRefresh = viewModel::refresh,
                            onEventClick = onEventClick,
                            sections = sections,
                            onFavoritesClick = onFavoritesClick,
                            favoriteAction = { event ->
                                val selected = event.id in favorites.ids
                                val colors = MaterialTheme.colorScheme
                                CompositionLocalProvider(LocalContentColor provides if (selected) colors.tertiary else colors.onSurfaceVariant) {
                                    FavoriteButton(selected, favorites.canChange(event.id)) {
                                        onFavoriteChange(event, !selected)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.primaryContainer,
            labelColor = colors.onPrimaryContainer,
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary
        )
    )
}

@Composable
internal fun ExploreContent(
    state: ExploreUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onEventClick: (Long) -> Unit,
    sections: @Composable () -> Unit,
    onFavoritesClick: (() -> Unit)? = null,
    favoriteAction: (@Composable (Event) -> Unit)? = null
) {
    val filters = state.filters
    val content = state.content
    val events = content?.events
    val downloadedAt = content?.downloadedAt
    ExplorePullToRefresh(
        onRefresh = onRefresh,
        isLoading = state.isLoading,
        hasScrollableContent = !events.isNullOrEmpty() || state.isLoading
    ) {
        ExploreSearch(filters.query, onQueryChange, stringResource(R.string.catalog_search))
        sections()
        val categories = content?.categories.orEmpty()
        if (categories.isNotEmpty() || filters.isActive) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.catalog_categories),
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filters.isActive && (events?.isEmpty() != true || state.hasError)) {
                    TextButton(onClick = onClearFilters) { Text(stringResource(R.string.catalog_clear_filters)) }
                }
            }
        }
        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CategoryPill(
                        stringResource(R.string.catalog_all_categories),
                        selected = filters.categoryId == null,
                        onClick = { filters.categoryId?.let(onCategorySelect) }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    CategoryPill(
                        category.name,
                        selected = filters.categoryId == category.id,
                        onClick = { onCategorySelect(category.id) }
                    )
                }
            }
        }
        if (events != null && downloadedAt != null) {
            CachedDataNotice(
                cachedCatalogNotice(downloadedAt),
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (state.isLoading) {
            val loadingLabel = stringResource(R.string.catalog_loading)
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .semantics { contentDescription = loadingLabel }
            )
        }
        if (state.hasError && events != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        ExploreEventCard(event, onEventClick, favoriteAction)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreEventCard(event: Event, onEventClick: (Long) -> Unit, favorite: (@Composable (Event) -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    ExploreCard(
        imageUrl = event.imageUrl,
        fallback = R.drawable.ic_catalog_ticket,
        onClick = { onEventClick(event.id) },
        cover = {
            ExploreBadge(
                event.category.name,
                Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp, end = 64.dp),
                icon = R.drawable.ic_catalog_ticket
            )
            if (favorite != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = colors.surface
                ) { favorite(event) }
            }
        }
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(painterResource(R.drawable.ic_event_calendar), null, Modifier.size(16.dp), tint = colors.primary)
                Text(eventLocalDateTime(event), style = MaterialTheme.typography.labelLarge, color = colors.primary)
            }
            val spots = event.availableSpots
            val soldOut = spots <= 0
            ExploreBadge(
                label = if (soldOut) stringResource(R.string.catalog_sold_out)
                else pluralStringResource(R.plurals.catalog_spots_available, spots, spots),
                containerColor = if (soldOut) colors.errorContainer else colors.primaryContainer,
                contentColor = if (soldOut) colors.onErrorContainer else colors.onPrimaryContainer
            )
        }
        Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val venue = event.venue
        val location = if (venue.address.isBlank()) venue.name
        else stringResource(R.string.catalog_event_venue, venue.name, venue.address)
        if (location.isNotBlank()) ExploreMetadata(R.drawable.ic_catalog_pin, location)
    }
}

@Composable
private fun CatalogSkeleton(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.surfaceContainerHighest
    val shape = RoundedCornerShape(8.dp)
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column {
                    Box(Modifier.fillMaxWidth().height(144.dp).background(tint))
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.fillMaxWidth(0.35f).height(12.dp).background(tint, shape))
                        Box(Modifier.fillMaxWidth(0.9f).height(18.dp).background(tint, shape))
                        Box(Modifier.fillMaxWidth(0.6f).height(12.dp).background(tint, shape))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExplorePreview() {
    val music = Category(1, "Música")
    val technology = Category(2, "Tecnología")
    val event = Event(
        id = 1,
        title = "Noche de jazz en la plaza",
        summary = "",
        description = null,
        startsAt = OffsetDateTime.parse("2027-11-14T20:00:00-04:00"),
        endsAt = null,
        category = music,
        imageUrl = null,
        venue = Venue(1, "Casa de la Cultura", "Av. Principal 123"),
        capacity = 60,
        availableSpots = 36,
        isAttending = null
    )
    ExploreStatePreview(
        ExploreUiState(
            content = ExploreUiState.Content(
                listOf(music, Category(3, "Cultura"), technology),
                listOf(
                    event,
                    event.copy(
                        id = 2,
                        title = "Taller de Robótica y Kotlin",
                        category = technology,
                        availableSpots = 0
                    )
                )
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
    ExplorePreviewFrame {
        ExploreContent(
            state, {}, {}, {}, {}, {},
            sections = { ExploreSections(showVenues = false, onSelect = {}) },
            favoriteAction = { FavoriteButton(it.id == 1L, true) {} }
        )
    }
}
