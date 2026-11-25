package com.example.eventia.data.local.model

import androidx.room.Embedded
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.time.OffsetDateTime

data class StoredEvent(
    val title: String,
    val summary: String,
    val description: String?,
    val startsAt: String,
    val endsAt: String?,
    @Embedded(prefix = "category_") val category: Category,
    val imageUrl: String?,
    @Embedded(prefix = "venue_") val venue: Venue,
    val capacity: Int,
    val availableSpots: Int
) {
    fun toDomain(id: Long): Event = Event(
        id, title,
        summary,
        description,
        OffsetDateTime.parse(startsAt),
        endsAt?.let(OffsetDateTime::parse),
        category,
        imageUrl,
        venue,
        capacity,
        availableSpots,
        isAttending = null
    )

    companion object {
        fun from(event: Event): StoredEvent = StoredEvent(
            event.title,
            event.summary,
            event.description,
            event.startsAt.toString(),
            event.endsAt?.toString(),
            event.category,
            event.imageUrl,
            event.venue,
            event.capacity,
            event.availableSpots
        )
    }
}
