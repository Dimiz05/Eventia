package com.example.eventia.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.repositories.AttendanceRepository
import com.example.eventia.repositories.AttendanceSessionUnavailable
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

internal data class AttendanceGroups(val upcoming: List<Event>, val past: List<Event>) {
    companion object {
        fun from(events: List<Event>, now: Instant): AttendanceGroups {
            val (upcoming, past) = events.sortedBy { it.startsAt.toInstant() }
                .partition { !it.startsAt.toInstant().isBefore(now) }
            return AttendanceGroups(upcoming, past.asReversed())
        }
    }
}

internal data class ProfileUiState(
    val attendances: AttendanceGroups? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val sessionRejected: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AttendanceRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    internal val state = mutableState.asStateFlow()
    private var request: Job? = null

    fun refresh() {
        request?.cancel()
        mutableState.value = mutableState.value.copy(loading = true, failed = false, sessionRejected = false)
        request = viewModelScope.launch {
            try {
                val events = repository.getMyAttendances()
                mutableState.value = ProfileUiState(AttendanceGroups.from(events, Instant.now()), loading = false)
            } catch (error: HttpException) {
                if (error.code() == 401) sessionRejected() else loadFailed()
            } catch (_: AttendanceSessionUnavailable) {
                sessionRejected()
            } catch (_: IOException) {
                loadFailed()
            } catch (_: JsonParseException) {
                loadFailed()
            } catch (_: DateTimeParseException) {
                loadFailed()
            }
        }
    }

    private fun loadFailed() {
        mutableState.value = mutableState.value.copy(loading = false, failed = true)
    }

    private fun sessionRejected() {
        mutableState.value = ProfileUiState(loading = false, failed = true, sessionRejected = true)
    }
}
