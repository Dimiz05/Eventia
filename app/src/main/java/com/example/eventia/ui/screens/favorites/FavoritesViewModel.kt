package com.example.eventia.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.repositories.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class FavoritesUiState(
    val events: List<Event>? = null,
    val ids: Set<Long> = emptySet(),
    val hasError: Boolean = false,
    val pendingIds: Set<Long> = emptySet()
) {
    fun canChange(id: Long): Boolean = events != null && !hasError && id !in pendingIds
}

internal sealed interface FavoriteNotice {
    data object Saved : FavoriteNotice
    data class Removed(val event: Event, val offerUndo: Boolean) : FavoriteNotice
    data class Failed(val retry: () -> Unit) : FavoriteNotice
}

@HiltViewModel
internal class FavoritesViewModel @Inject constructor(
    private val repository: FavoritesRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(FavoritesUiState())
    val state = mutableState.asStateFlow()
    private val noticeChannel = Channel<FavoriteNotice>(Channel.BUFFERED)
    val notices = noticeChannel.receiveAsFlow()

    init {
        observeFavorites()
    }

    fun retry() {
        if (!state.value.hasError) return
        mutableState.update { it.copy(hasError = false, events = null) }
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.observeFavorites()
                .catch { mutableState.update { it.copy(hasError = true) } }
                .collect { events ->
                    val ids = events.mapTo(mutableSetOf(), Event::id)
                    mutableState.update { it.copy(events = events, ids = ids, hasError = false) }
                }
        }
    }

    fun changeFavorite(event: Event, save: Boolean, offerUndo: Boolean = false) {
        if (event.id in state.value.pendingIds) return
        mutableState.update { it.copy(pendingIds = it.pendingIds + event.id) }
        viewModelScope.launch {
            try {
                if (save) {
                    repository.addFavorite(event)
                    noticeChannel.send(FavoriteNotice.Saved)
                } else {
                    val removed = repository.removeFavorite(event.id)
                    if (removed != null) noticeChannel.send(FavoriteNotice.Removed(removed, offerUndo))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                noticeChannel.send(FavoriteNotice.Failed { changeFavorite(event, save, offerUndo) })
            } finally {
                mutableState.update { it.copy(pendingIds = it.pendingIds - event.id) }
            }
        }
    }
}
