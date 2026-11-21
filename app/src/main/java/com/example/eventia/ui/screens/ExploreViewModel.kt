package com.example.eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.repositories.CatalogRepository
import com.google.gson.JsonParseException
import com.example.eventia.domain.model.Category
import com.example.eventia.repositories.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

sealed interface ExploreUiState {
    data object Loading : ExploreUiState
    data class Content(
        val categories: List<Category>,
        val events: List<Event>,
        val venues: List<Venue>
    ) : ExploreUiState
    data object Empty : ExploreUiState
    data object Error : ExploreUiState
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val categories: CategoryRepository,
    private val catalog: CatalogRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val state = _state.asStateFlow()

    init {
        loadCatalog()
    }

    fun retry() {
        if (_state.value == ExploreUiState.Error) loadCatalog()
    }

    private fun loadCatalog() {
        _state.value = ExploreUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val categoryList = categories.getCategories()
                val allEvents = catalog.getEvents()
                val venues = catalog.getVenues()
                val now = Instant.now()
                val events = allEvents.filter { it.startsAt.toInstant().isAfter(now) }
                if (categoryList.isEmpty() && events.isEmpty() && venues.isEmpty()) {
                    ExploreUiState.Empty
                } else {
                    ExploreUiState.Content(categoryList, events, venues)
                }
            } catch (_: IOException) {
                ExploreUiState.Error
            } catch (_: HttpException) {
                ExploreUiState.Error
            } catch (_: DateTimeParseException) {
                ExploreUiState.Error
            } catch (_: JsonParseException) {
                ExploreUiState.Error
            }
        }
    }
}
