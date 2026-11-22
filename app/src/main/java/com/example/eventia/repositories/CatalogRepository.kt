package com.example.eventia.repositories

import com.example.eventia.api.EventiaApi
import com.example.eventia.api.models.EventDto
import com.example.eventia.api.models.VenueDto
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.time.OffsetDateTime
import javax.inject.Inject

interface CatalogRepository {
    suspend fun getEvents(query: String? = null, categoryId: Long? = null): List<Event>
    suspend fun getEvent(id: Long): Event
    suspend fun getVenues(): List<Venue>
    suspend fun getVenue(id: Long): Venue
    suspend fun getVenueEvents(id: Long): List<Event>
}

class RemoteCatalogRepository @Inject constructor(
    private val api: EventiaApi
) : CatalogRepository {
    override suspend fun getEvents(query: String?, categoryId: Long?): List<Event> =
        api.getEvents(query, categoryId).map(EventDto::toDomain)
    override suspend fun getEvent(id: Long): Event = api.getEvent(id).toDomain()
    override suspend fun getVenues(): List<Venue> = api.getVenues().map(VenueDto::toDomain)
    override suspend fun getVenue(id: Long): Venue = api.getVenue(id).toDomain()
    override suspend fun getVenueEvents(id: Long): List<Event> =
        api.getVenueEvents(id).map(EventDto::toDomain)
}

internal fun EventDto.toDomain(): Event = Event(
    id = id,
    title = title,
    summary = summary,
    description = description,
    startsAt = OffsetDateTime.parse(startsAt),
    endsAt = endsAt?.let(OffsetDateTime::parse),
    category = Category(category.id, category.name),
    imageUrl = imageUrl,
    venue = venue.toDomain(),
    capacity = capacity,
    availableSpots = availableSpots,
    isAttending = isAttending
)

internal fun VenueDto.toDomain(): Venue = Venue(
    id = id,
    name = name,
    address = address,
    description = description,
    openingHours = openingHours,
    imageUrl = imageUrl,
    latitude = lat,
    longitude = lng,
    upcomingEventsCount = upcomingEventsCount
)
