package com.example.eventia.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.repositories.CatalogRepository
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.gson.JsonParseException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed interface MapUiState {
    data object NotConfigured : MapUiState
    data object ServicesUnavailable : MapUiState
    data class Ready(
        val attempt: Int,
        val categoryId: Long?,
        val categories: List<Category> = emptyList(),
        val markers: Map<MapPoint, List<Event>> = emptyMap(),
        val selectedPoint: MapPoint? = null,
        val isLoading: Boolean = true,
        val hasError: Boolean = false
    ) : MapUiState
}

data class MapPoint(val latitude: Double, val longitude: Double) {
    companion object {
        internal fun from(latitude: Double?, longitude: Double?): MapPoint? {
            if (latitude == null || longitude == null ||
                latitude !in -90.0..90.0 || longitude !in -180.0..180.0
            ) return null

            // Match the map's antimeridian and group positive and negative zero together.
            val normalizedLongitude = if (longitude == 180.0) -180.0 else longitude + 0.0
            return MapPoint(latitude + 0.0, normalizedLongitude)
        }
    }
}

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: CatalogRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private var attempt = 0
    private var events = emptyList<Event>()
    private val _state = MutableStateFlow(checkAvailability())
    val state = _state.asStateFlow()

    init {
        if (state.value is MapUiState.Ready) loadEvents()
    }

    fun retryMap() {
        val previous = state.value as? MapUiState.Ready
        attempt++
        val availability = checkAvailability()
        _state.value = if (availability is MapUiState.Ready && previous != null) {
            previous.copy(attempt = attempt, selectedPoint = null)
        } else {
            availability
        }
        if (availability is MapUiState.Ready && previous == null) loadEvents()
    }

    fun retryEvents() {
        val current = state.value as? MapUiState.Ready ?: return
        if (!current.isLoading) loadEvents()
    }

    fun selectCategory(categoryId: Long?) {
        val current = state.value as? MapUiState.Ready ?: return
        savedState[CategoryKey] = categoryId
        _state.value = current.copy(
            categoryId = categoryId,
            markers = markersFor(categoryId),
            selectedPoint = null
        )
    }

    fun selectPoint(point: MapPoint?) {
        val current = state.value as? MapUiState.Ready ?: return
        _state.value = current.copy(selectedPoint = point?.takeIf(current.markers::containsKey))
    }

    private fun loadEvents() {
        val current = state.value as? MapUiState.Ready ?: return
        _state.value = current.copy(isLoading = true, hasError = false)
        viewModelScope.launch {
            val result = try {
                catalog.getEvents()
            } catch (_: IOException) {
                null
            } catch (_: HttpException) {
                null
            } catch (_: DateTimeParseException) {
                null
            } catch (_: JsonParseException) {
                null
            }
            val latest = state.value as? MapUiState.Ready ?: return@launch
            if (result == null) {
                _state.value = latest.copy(isLoading = false, hasError = true)
            } else {
                events = result
                val categories = events.map(Event::category).distinctBy(Category::id)
                val categoryId = latest.categoryId?.takeIf { id -> categories.any { it.id == id } }
                savedState[CategoryKey] = categoryId
                val markers = markersFor(categoryId)
                _state.value = latest.copy(
                    categoryId = categoryId,
                    categories = categories,
                    markers = markers,
                    selectedPoint = latest.selectedPoint?.takeIf(markers::containsKey),
                    isLoading = false
                )
            }
        }
    }

    private fun markersFor(categoryId: Long?): Map<MapPoint, List<Event>> =
        events.asSequence()
            .filter { categoryId == null || it.category.id == categoryId }
            .mapNotNull { event ->
                MapPoint.from(event.venue.latitude, event.venue.longitude)?.let { it to event }
            }
            .groupBy({ it.first }, { it.second })

    private fun checkAvailability(): MapUiState {
        val metadata = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        ).metaData
        if (metadata?.getString("com.google.android.geo.API_KEY").isNullOrBlank()) {
            return MapUiState.NotConfigured
        }
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        return if (availability == ConnectionResult.SUCCESS) {
            MapUiState.Ready(attempt, savedState[CategoryKey])
        } else {
            MapUiState.ServicesUnavailable
        }
    }

    private companion object {
        const val CategoryKey = "mapCategory"
    }
}
