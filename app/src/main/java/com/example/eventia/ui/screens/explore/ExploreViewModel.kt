package com.example.eventia.ui.screens.explore

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.data.connectivity.CatalogConnectivity
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.repositories.CatalogEvents
import com.example.eventia.repositories.CatalogRepository
import com.example.eventia.repositories.CategoryRepository
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class ExploreFilters(val query: String = "", val categoryId: Long? = null) {
    val isActive: Boolean get() = query.isNotBlank() || categoryId != null
}

data class ExploreUiState(
    val filters: ExploreFilters = ExploreFilters(),
    val content: Content? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false
) {
    data class Content(
        val categories: List<Category>,
        val events: List<Event>?,
        val downloadedAt: Instant? = null
    )
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val categories: CategoryRepository,
    private val catalog: CatalogRepository,
    private val savedState: SavedStateHandle,
    connectivity: CatalogConnectivity
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ExploreUiState(ExploreFilters(savedState[QUERY] ?: "", savedState[CATEGORY]))
    )
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var catalogRefreshPending = false

    init {
        viewModelScope.launch {
            connectivity.reconnects().collect {
                catalogRefreshPending = true
                load(reloadCatalog = true)
            }
        }
        load(reloadCatalog = true)
    }

    fun changeQuery(query: String) {
        updateFilters(state.value.filters.copy(query = query), waitForInput = true)
    }

    fun selectCategory(categoryId: Long) {
        val filters = state.value.filters
        val selection = categoryId.takeUnless { it == filters.categoryId }
        updateFilters(filters.copy(categoryId = selection), waitForInput = true)
    }

    fun clearFilters() {
        updateFilters(ExploreFilters(), waitForInput = false)
    }

    fun refresh() {
        if (!state.value.isLoading) load(reloadCatalog = true)
    }

    private fun updateFilters(filters: ExploreFilters, waitForInput: Boolean) {
        val previous = state.value
        if (previous.filters == filters) return
        savedState[QUERY] = filters.query
        savedState[CATEGORY] = filters.categoryId
        mutableState.value = previous.copy(
            filters = filters,
            content = previous.content?.copy(events = null),
            isLoading = true,
            hasError = false
        )
        load(reloadCatalog = false, waitForInput = waitForInput)
    }

    private fun load(reloadCatalog: Boolean, waitForInput: Boolean = false) {
        request?.cancel()
        val current = state.value.copy(isLoading = true, hasError = false)
        mutableState.value = current
        request = viewModelScope.launch {
            if (waitForInput) delay(SEARCH_DELAY_MS)
            val content = try {
                fetchContent(current, reloadCatalog || catalogRefreshPending)
            } catch (_: IOException) {
                null
            } catch (_: HttpException) {
                null
            } catch (_: DateTimeParseException) {
                null
            } catch (_: JsonParseException) {
                null
            } catch (_: SQLiteException) {
                null
            }
            ensureActive()
            mutableState.value = current.copy(
                content = content ?: current.content,
                isLoading = false,
                hasError = content == null
            )
        }
    }

    private suspend fun fetchContent(current: ExploreUiState, reloadCatalog: Boolean): ExploreUiState.Content {
        val previousRemote = current.content?.takeIf { !reloadCatalog && it.downloadedAt == null }
        val filters = current.filters
        val query = filters.query.trim().ifEmpty { null }
        val unfiltered = query == null && filters.categoryId == null
        if (catalogRefreshPending && !unfiltered) {
            refreshCatalogCache()
        }
        val result = catalog.loadEvents(query, filters.categoryId)
        if (unfiltered && result is CatalogEvents.Remote) {
            catalogRefreshPending = false
        }
        val now = Instant.now()
        val events = result.events.filter { it.startsAt.toInstant().isAfter(now) }
        return when (result) {
            is CatalogEvents.Remote -> ExploreUiState.Content(
                previousRemote?.categories ?: categories.getCategories(),
                events
            )
            is CatalogEvents.Cached -> ExploreUiState.Content(
                result.categories,
                events,
                result.downloadedAt
            )
        }
    }

    private suspend fun refreshCatalogCache() {
        if (catalog.loadEvents(null, null) is CatalogEvents.Remote) {
            catalogRefreshPending = false
        }
    }

    private companion object {
        const val QUERY = "exploreQuery"
        const val CATEGORY = "exploreCategory"
        const val SEARCH_DELAY_MS = 300L
    }
}
