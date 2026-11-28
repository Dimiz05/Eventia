package com.example.eventia.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.eventia.R
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.components.CachedDataNotice
import com.example.eventia.ui.components.eventLocalDateTime
import com.example.eventia.ui.components.EventArtwork
import com.example.eventia.ui.components.loadingSemantics
import com.example.eventia.ui.components.favoriteIcon
import com.example.eventia.ui.components.favoriteLabel
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.components.cachedCatalogNotice
import com.example.eventia.domain.model.Venue
import com.example.eventia.ui.screens.favorites.FavoritesUiState
import com.example.eventia.ui.screens.venue.VenueListScreen
import java.time.OffsetDateTime
import java.util.Locale

@Composable
internal fun ExploreScreen(
    onEventClick: (Long) -> Unit,
    onVenueClick: (Long) -> Unit,
    favorites: FavoritesUiState,
    onFavoriteChange: (Event, Boolean) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
    onFavoritesClick: (() -> Unit)? = null,
    avatar: @Composable () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showVenues by rememberSaveable { mutableStateOf(false) }
    val sectionState = rememberSaveableStateHolder()
    val sections: @Composable () -> Unit = { ExploreSections(showVenues) { showVenues = it } }
    MaterialTheme(colorScheme = exploreColors()) {
        Surface(Modifier.fillMaxSize()) {
            Column {
                ExploreHeader(avatar)
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
                                ExploreFavoriteButton(selected, favorites.canChange(event.id)) {
                                    onFavoriteChange(event, !selected)
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
private fun CategoryTile(
    label: String,
    icon: Int,
    selected: Boolean,
    enabled: Boolean = true,
    opensMenu: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val labelColor = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.62f)
    val iconBackground = if (enabled) colors.onPrimaryContainer else colors.onSurfaceVariant
    Surface(
        shape = CircleShape,
        color = if (selected) colors.surfaceContainerHigh else colors.surface,
        border = if (selected) BorderStroke(1.dp, colors.outline) else null,
        shadowElevation = 2.dp
    ) {
        Row(
            Modifier.selectable(selected, enabled = enabled,
                role = if (opensMenu) Role.Button else Role.RadioButton, onClick = onClick)
                .heightIn(min = 48.dp).padding(start = 6.dp, end = 7.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(28.dp).background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), null, Modifier.size(16.dp), tint = colors.onPrimary)
            }
            Text(label, color = labelColor, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

internal fun categoryIcon(name: String): Int = when (name.trim().lowercase(Locale.ROOT)) {
    "arte" -> R.drawable.ic_category_art
    "deporte" -> R.drawable.ic_category_sport
    "gastronomía", "gastronomia" -> R.drawable.ic_category_food
    "música", "musica" -> R.drawable.ic_category_music
    "taller", "talleres" -> R.drawable.ic_category_workshop
    "teatro" -> R.drawable.ic_category_theater
    "tecnología", "tecnologia" -> R.drawable.ic_category_technology
    else -> R.drawable.ic_category_tag
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
        ExploreSearch(filters.query, onQueryChange, stringResource(R.string.catalog_search), inlineHint = true)
        sections()
        val categories = content?.categories.orEmpty()
        val music = categories.firstOrNull { categoryIcon(it.name) == R.drawable.ic_category_music }
        val food = categories.firstOrNull { categoryIcon(it.name) == R.drawable.ic_category_food }
        val remaining = categories.filter { it.id != music?.id && it.id != food?.id }
        val extraSelection = remaining.firstOrNull { it.id == filters.categoryId }
        var showFilters by rememberSaveable { mutableStateOf(false) }
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup().padding(top = 5.dp, bottom = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                CategoryTile(stringResource(R.string.catalog_all_categories),
                    R.drawable.ic_category_all, filters.categoryId == null) {
                    filters.categoryId?.let(onCategorySelect)
                }
            }
            item {
                CategoryTile(stringResource(R.string.catalog_music), R.drawable.ic_category_music,
                    music != null && filters.categoryId == music.id, enabled = music != null) {
                    music?.let { onCategorySelect(it.id) }
                }
            }
            item {
                CategoryTile(stringResource(R.string.catalog_food), R.drawable.ic_category_food,
                    food != null && filters.categoryId == food.id, enabled = food != null) {
                    food?.let { onCategorySelect(it.id) }
                }
            }
            item {
                Box {
                    val label = if (extraSelection == null) stringResource(R.string.catalog_more_filters)
                    else stringResource(R.string.catalog_active_more_filters, extraSelection.name)
                    CategoryTile(label, R.drawable.ic_catalog_filters, extraSelection != null,
                        enabled = remaining.isNotEmpty() || filters.isActive, opensMenu = true) {
                        showFilters = true
                    }
                    DropdownMenu(expanded = showFilters, onDismissRequest = { showFilters = false }) {
                        remaining.forEach { category ->
                            val selected = category.id == filters.categoryId
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                leadingIcon = { Icon(painterResource(categoryIcon(category.name)), null) },
                                trailingIcon = if (selected) {
                                    { Icon(painterResource(R.drawable.ic_session_check), null) }
                                } else null,
                                onClick = {
                                    onCategorySelect(category.id)
                                    showFilters = false
                                }
                            )
                        }
                        if (filters.isActive) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.catalog_clear_filters)) },
                                onClick = {
                                    onClearFilters()
                                    showFilters = false
                                })
                        }
                    }
                }
            }
        }
        if (events != null && downloadedAt != null) {
            CachedDataNotice(
                cachedCatalogNotice(downloadedAt),
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (state.isLoading && !events.isNullOrEmpty()) {
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
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val spacing = 8.dp
                    val density = LocalDensity.current
                    val minimumHeight = with(density) { 55.dp + 68.sp.toDp() }
                    val fittedHeight = with(density) {
                        // Round the padding and gap separately, as LazyColumn does.
                        val available = constraints.maxHeight - 4 * spacing.roundToPx()
                        // Round up so the fourth card begins beyond the viewport.
                        ((available.coerceAtLeast(0) + 2) / 3).toDp()
                    }
                    val cardHeight = fittedHeight.coerceAtLeast(minimumHeight)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = spacing),
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(events, key = { it.id }) { event ->
                            ExploreEventCard(event, onEventClick, favoriteAction, cardHeight, multiline = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExploreEventCard(
    event: Event,
    onEventClick: (Long) -> Unit,
    favorite: (@Composable (Event) -> Unit)?,
    height: Dp,
    multiline: Boolean
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = { onEventClick(event.id) },
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 2.dp
    ) {
        Box(Modifier.fillMaxSize()) {
            EventArtwork(event.imageUrl, Modifier.matchParentSize())
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
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        Modifier.background(colors.scrim.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_event_calendar), null,
                            Modifier.size(15.dp), tint = Color.White)
                        Text(eventLocalDateTime(event, compact = true),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                favorite?.invoke(event)
            }
            BoxWithConstraints(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().panoramicCaptionScrim().padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                val categoryMaxWidth = maxWidth * 0.38f
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            event.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFFFFF),
                            maxLines = if (multiline) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            event.venue.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        modifier = Modifier.widthIn(max = categoryMaxWidth),
                        shape = RoundedCornerShape(50),
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    ) {
                        Text(
                            event.category.name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExploreFavoriteButton(selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (selected) colors.primary else colors.onSurfaceVariant
    val container = colors.surface
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = container) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(favoriteIcon(selected)),
                    stringResource(favoriteLabel(selected)),
                    Modifier.size(28.dp),
                    tint = tint.copy(alpha = if (enabled) 1f else 0.5f)
                )
            }
        }
    }
}

@Composable
private fun CatalogSkeleton(modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.surfaceContainerHighest
    val shape = RoundedCornerShape(8.dp)
    LazyColumn(
        modifier = modifier.fillMaxWidth().loadingSemantics(stringResource(R.string.catalog_loading)),
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
            favoriteAction = { ExploreFavoriteButton(it.id == 2L, true) {} }
        )
    }
}
