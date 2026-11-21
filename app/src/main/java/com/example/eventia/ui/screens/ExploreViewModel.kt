package com.example.eventia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed interface ExploreUiState {
    data object Loading : ExploreUiState
    data class Content(val categories: List<Category>) : ExploreUiState
    data object Empty : ExploreUiState
    data object Error : ExploreUiState
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val state = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun retry() {
        if (_state.value == ExploreUiState.Error) loadCategories()
    }

    private fun loadCategories() {
        _state.value = ExploreUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val categories = repository.getCategories()
                if (categories.isEmpty()) ExploreUiState.Empty else ExploreUiState.Content(categories)
            } catch (_: IOException) {
                ExploreUiState.Error
            } catch (_: HttpException) {
                ExploreUiState.Error
            } catch (_: JsonParseException) {
                ExploreUiState.Error
            }
        }
    }
}
