package com.example.eventia.ui.screens.eventdetail

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.hasStarted
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.repositories.EventSessionExpired
import com.example.eventia.repositories.SessionRepository
import com.example.eventia.ui.navigation.DetailId
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Instant
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
        val refreshRequired: Boolean = false,
        val conflict: AttendanceConflict? = null,
        val attendanceConfirmation: Boolean? = null
    ) : EventDetailUiState
    data object Empty : EventDetailUiState
    data object Error : EventDetailUiState
}

internal enum class AttendanceConflict {
    PendingRegistration, Full, AlreadyRegistered, Unconfirmed
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

    fun dismissConflict() {
        val content = mutableState.value as? EventDetailUiState.Content ?: return
        mutableState.value = content.copy(conflict = null)
    }

    fun attend(onSessionExpired: (Long) -> Unit, onAttendanceChanged: () -> Unit = {}) {
        val content = mutableState.value as? EventDetailUiState.Content ?: return
        val attending = content.event.isAttending ?: return
        if (!content.authenticated || content.updating || content.refreshRequired) return
        if (content.event.hasStarted(Instant.now())) return
        val previous = content.copy(attendanceError = false, conflict = null, attendanceConfirmation = null)
        mutableState.value = previous.copy(updating = true)
        request = viewModelScope.launch {
            try {
                if (attending) session.cancelAttendance(eventId) else session.attendEvent(eventId)
            } catch (_: EventSessionExpired) {
                onSessionExpired(eventId)
                return@launch
            } catch (_: IOException) {
                mutableState.value = previous.copy(attendanceError = true, refreshRequired = true)
                return@launch
            } catch (error: HttpException) {
                if (error.code() == 409) {
                    val conflict = if (attending) AttendanceConflict.Unconfirmed
                    else AttendanceConflict.PendingRegistration
                    loadEvent(true, onSessionExpired, previous.copy(conflict = conflict))
                } else {
                    mutableState.value = previous.copy(attendanceError = true, refreshRequired = true)
                }
                return@launch
            }
            onAttendanceChanged()
            loadEvent(true, onSessionExpired, previous, expectedAttendance = !attending)
        }
    }

    private suspend fun loadEvent(
        authenticated: Boolean,
        onSessionExpired: (Long) -> Unit,
        previous: EventDetailUiState.Content? = null,
        expectedAttendance: Boolean? = null
    ) {
        val failure = previous?.copy(updating = false, refreshRequired = true) ?: EventDetailUiState.Error
        mutableState.value = try {
            val event = catalog.getEvent(eventId, authenticated, allowSaved = previous == null)
            EventDetailUiState.Content(
                event,
                authenticated,
                refreshRequired = authenticated && event.isAttending == null,
                attendanceConfirmation = expectedAttendance?.takeIf { it == event.isAttending },
                conflict = when {
                    expectedAttendance != null && expectedAttendance != event.isAttending -> AttendanceConflict.Unconfirmed
                    previous?.conflict != AttendanceConflict.PendingRegistration -> previous?.conflict
                    event.isAttending == null -> AttendanceConflict.PendingRegistration
                    event.isAttending -> AttendanceConflict.AlreadyRegistered
                    event.availableSpots <= 0 -> AttendanceConflict.Full
                    else -> AttendanceConflict.Unconfirmed
                }
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
