package com.example.eventia.repositories

import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.time.Instant

interface CatalogRepository {
    suspend fun getEvents(query: String? = null, categoryId: Long? = null): List<Event>
    suspend fun loadEvents(query: String?, categoryId: Long?): CatalogEvents
    suspend fun getEvent(id: Long, authenticated: Boolean = false, allowSaved: Boolean = true): Event
    suspend fun getVenues(): List<Venue>
    suspend fun getVenue(id: Long): Venue
    suspend fun getVenueEvents(id: Long): List<Event>
}

sealed interface CatalogEvents {
    val events: List<Event>

    data class Remote(override val events: List<Event>) : CatalogEvents

    data class Cached(
        override val events: List<Event>,
        val categories: List<Category>,
        val downloadedAt: Instant
    ) : CatalogEvents
}
