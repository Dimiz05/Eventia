package com.example.eventia.ui.screens.venue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.ui.navigation.DetailId
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal data class VenueListUiState(
    val query: String = "",
    val venues: List<Venue>? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false
) {
    val matches: List<Venue>
        get() = venues.orEmpty().filter { it.name.contains(query.trim(), ignoreCase = true) }
}

@HiltViewModel
class VenueListViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(VenueListUiState(query = savedState[QUERY] ?: ""))
    internal val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun changeQuery(query: String) {
        savedState[QUERY] = query
        mutableState.update { it.copy(query = query) }
    }

    fun refresh() {
        if (state.value.isLoading) return
        mutableState.update { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            val result = venueRequest { catalog.getVenues() }
            mutableState.update {
                it.copy(
                    venues = result.getOrNull() ?: it.venues,
                    isLoading = false,
                    hasError = result.isFailure
                )
            }
        }
    }

    private companion object {
        const val QUERY = "venueQuery"
    }
}

internal sealed interface VenueDetailUiState {
    data object Loading : VenueDetailUiState
    data object Empty : VenueDetailUiState
    data object Error : VenueDetailUiState
    data class Content(val venue: Venue, val events: VenueEventsUiState) : VenueDetailUiState
}

internal sealed interface VenueEventsUiState {
    data object Loading : VenueEventsUiState
    data object Error : VenueEventsUiState
    data class Loaded(val events: List<Event>) : VenueEventsUiState
}

@HiltViewModel
class VenueDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val catalog: CatalogRepository
) : ViewModel() {
    private val venueId = requireNotNull(savedState.get<Long>(DetailId))
    private val mutableState = MutableStateFlow<VenueDetailUiState>(VenueDetailUiState.Loading)
    internal val state = mutableState.asStateFlow()

    init {
        loadVenue()
    }

    fun retry() {
        when (val current = state.value) {
            VenueDetailUiState.Error -> loadVenue()
            is VenueDetailUiState.Content -> if (current.events == VenueEventsUiState.Error) {
                mutableState.value = current.copy(events = VenueEventsUiState.Loading)
                viewModelScope.launch { loadEvents(current.venue) }
            }
            else -> Unit
        }
    }

    private fun loadVenue() {
        mutableState.value = VenueDetailUiState.Loading
        viewModelScope.launch {
            val result = venueRequest { catalog.getVenue(venueId) }
            val venue = result.getOrNull()
            if (venue != null) {
                mutableState.value = VenueDetailUiState.Content(venue, VenueEventsUiState.Loading)
                loadEvents(venue)
            } else {
                val status = (result.exceptionOrNull() as? HttpException)?.code()
                mutableState.value = if (status == 404) VenueDetailUiState.Empty else VenueDetailUiState.Error
            }
        }
    }

    private suspend fun loadEvents(venue: Venue) {
        val result = venueRequest { catalog.getVenueEvents(venueId) }
        val now = Instant.now()
        val events = result.getOrNull()?.filter { it.startsAt.toInstant().isAfter(now) }
        mutableState.value = VenueDetailUiState.Content(
            venue,
            if (events == null) VenueEventsUiState.Error else VenueEventsUiState.Loaded(events)
        )
    }
}

private suspend fun <T> venueRequest(request: suspend () -> T): Result<T> = try {
    Result.success(request())
} catch (error: IOException) {
    Result.failure(error)
} catch (error: HttpException) {
    Result.failure(error)
} catch (error: JsonParseException) {
    Result.failure(error)
} catch (error: DateTimeParseException) {
    Result.failure(error)
}
