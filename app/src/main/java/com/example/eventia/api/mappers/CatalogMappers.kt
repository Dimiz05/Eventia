package com.example.eventia.api.mappers

import com.example.eventia.api.models.EventDto
import com.example.eventia.api.models.VenueDto
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.time.OffsetDateTime

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
