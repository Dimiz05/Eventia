package com.example.eventia.ui.screens

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Event
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.ui.navigation.DetailId
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

internal sealed interface EventDetailUiState {
    data object Loading : EventDetailUiState
    data class Content(val event: Event) : EventDetailUiState
    data object Empty : EventDetailUiState
    data object Error : EventDetailUiState
}

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository
) : ViewModel() {
    private val eventId = requireNotNull(savedStateHandle.get<Long>(DetailId))
    private val mutableState = MutableStateFlow<EventDetailUiState>(EventDetailUiState.Loading)
    internal val state = mutableState.asStateFlow()

    init {
        loadEvent()
    }

    fun retry() {
        if (mutableState.value == EventDetailUiState.Error) loadEvent()
    }

    private fun loadEvent() {
        mutableState.value = EventDetailUiState.Loading
        viewModelScope.launch {
            mutableState.value = try {
                EventDetailUiState.Content(catalog.getEvent(eventId))
            } catch (error: HttpException) {
                if (error.code() == 404) EventDetailUiState.Empty else EventDetailUiState.Error
            } catch (_: IOException) {
                EventDetailUiState.Error
            } catch (_: DateTimeParseException) {
                EventDetailUiState.Error
            } catch (_: JsonParseException) {
                EventDetailUiState.Error
            } catch (_: SQLiteException) {
                EventDetailUiState.Error
            }
        }
    }
}
