package com.example.eventia.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.eventia.R
import com.example.eventia.domain.model.Event
import com.example.eventia.ui.components.CachedDataNotice
import com.example.eventia.ui.components.EventCard
import com.example.eventia.ui.components.FavoriteButton
import com.example.eventia.ui.components.LoadingScreen
import com.example.eventia.ui.components.ScreenMessage
import com.example.eventia.ui.theme.EventiaTheme

@Composable
internal fun FavoritesScreen(
    state: FavoritesUiState,
    onEventClick: (Long) -> Unit,
    onRemove: (Event) -> Unit,
    onRetry: () -> Unit
) {
    val events = state.events
    when {
        state.hasError -> ScreenMessage(
            title = stringResource(R.string.favorites_load_error),
            isError = true,
            actionLabel = stringResource(R.string.retry),
            onAction = onRetry
        )
        events == null -> LoadingScreen(stringResource(R.string.favorites_loading))
        events.isEmpty() -> ScreenMessage(title = stringResource(R.string.favorites_empty))
        else -> Column(Modifier.fillMaxSize()) {
            CachedDataNotice(
                stringResource(R.string.favorites_saved_data),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = Event::id) { event ->
                    EventCard(event, onEventClick) {
                        FavoriteButton(true, state.canChange(event.id)) { onRemove(event) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoritesEmptyPreview() {
    EventiaTheme {
        FavoritesScreen(FavoritesUiState(events = emptyList()), {}, {}, {})
    }
}
