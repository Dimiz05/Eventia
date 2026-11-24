package com.example.eventia.ui.screens

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.repositories.EventSessionExpired
import com.example.eventia.repositories.SessionRepository
import com.example.eventia.ui.navigation.DetailId
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Content(
        val event: Event,
        val authenticated: Boolean = false,
        val updating: Boolean = false,
        val attendanceError: Boolean = false,
        val refreshRequired: Boolean = false
    ) : EventDetailUiState
    data object Empty : EventDetailUiState
    data object Error : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val session: SessionRepository
) : ViewModel() {
    private val eventId = requireNotNull(savedStateHandle.get<Long>(DetailId))
    private val mutableState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    internal val state = mutableState.asStateFlow()
    private var request: Job? = null

    fun load(authenticated: Boolean, onSessionExpired: (Long) -> Unit) {
        cancelRequest()
        mutableState.value = EventDetailUiState.Loading
        request = viewModelScope.launch { loadEvent(authenticated, onSessionExpired) }
    }

    fun cancelRequest() {
        request?.cancel()
    }

    fun refresh(onSessionExpired: (Long) -> Unit) {
        val content = mutableState.value as? EventDetailUiState.Content ?: return
        if (content.updating || !content.refreshRequired) return
        mutableState.value = content.copy(updating = true)
        request = viewModelScope.launch { loadEvent(true, onSessionExpired, content) }
    }

    fun attend(onSessionExpired: (Long) -> Unit) {
        val content = mutableState.value as? EventDetailUiState.Content ?: return
        if (!content.authenticated || content.updating || content.refreshRequired ||
            content.event.isAttending != false
        ) return
        val previous = content.copy(attendanceError = false)
        mutableState.value = previous.copy(updating = true)
        request = viewModelScope.launch {
            try {
                session.attendEvent(eventId)
            } catch (_: EventSessionExpired) {
                onSessionExpired(eventId)
                return@launch
            } catch (_: IOException) {
                mutableState.value = previous.copy(attendanceError = true, refreshRequired = true)
                return@launch
            } catch (_: HttpException) {
                mutableState.value = previous.copy(attendanceError = true, refreshRequired = true)
                return@launch
            }
            loadEvent(true, onSessionExpired, previous)
        }
    }

    private suspend fun loadEvent(
        authenticated: Boolean,
        onSessionExpired: (Long) -> Unit,
        previous: EventDetailUiState.Content? = null
    ) {
        val failure = previous?.copy(updating = false, refreshRequired = true) ?: EventDetailUiState.Error
        mutableState.value = try {
            val event = catalog.getEvent(eventId, authenticated, allowSaved = previous == null)
            EventDetailUiState.Content(
                event,
                authenticated,
                refreshRequired = authenticated && event.isAttending == null
            )
        } catch (_: EventSessionExpired) {
            onSessionExpired(eventId)
            return
        } catch (error: HttpException) {
            if (error.code() == 404) EventDetailUiState.Empty else failure
        } catch (_: IOException) {
            failure
        } catch (_: DateTimeParseException) {
            failure
        } catch (_: JsonParseException) {
            failure
        } catch (_: SQLiteException) {
            failure
        }
    }
}
