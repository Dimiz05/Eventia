package com.example.eventia.repositories

import com.example.eventia.api.EventiaApi
import com.example.eventia.api.models.EventDto
import com.example.eventia.api.models.VenueDto
import com.example.eventia.data.local.CachedEventEntity
import com.example.eventia.data.local.CatalogCacheDao
import com.example.eventia.data.local.StoredEvent
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import retrofit2.HttpException

interface CatalogRepository {
    suspend fun getEvents(query: String? = null, categoryId: Long? = null): List<Event>
    suspend fun loadEvents(query: String?, categoryId: Long?): CatalogEvents
    suspend fun getEvent(id: Long): Event
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

class RemoteCatalogRepository @Inject constructor(
    private val api: EventiaApi,
    private val favorites: FavoritesRepository,
    private val cache: CatalogCacheDao
) : CatalogRepository {
    override suspend fun getEvents(query: String?, categoryId: Long?): List<Event> =
        api.getEvents(query, categoryId).map(EventDto::toDomain)

    override suspend fun loadEvents(query: String?, categoryId: Long?): CatalogEvents {
        val search = query?.trim()?.takeIf(String::isNotEmpty)
        val events = try {
            getEvents(search, categoryId)
        } catch (error: IOException) {
            return loadCachedEvents(search, categoryId) ?: throw error
        } catch (error: HttpException) {
            if (error.code() !in 500..599) throw error
            return loadCachedEvents(search, categoryId) ?: throw error
        }
        if (search == null && categoryId == null) {
            val downloadedAtMillis = System.currentTimeMillis()
            cache.replaceAll(
                events.map { CachedEventEntity(it.id, StoredEvent.from(it)) },
                downloadedAtMillis
            )
        }
        return CatalogEvents.Remote(events)
    }

    private suspend fun loadCachedEvents(query: String?, categoryId: Long?): CatalogEvents.Cached? {
        val snapshot = cache.getSnapshot() ?: return null
        val events = snapshot.events.map { it.details.toDomain(it.id) }
        val matches = events.filter { event ->
            (categoryId == null || event.category.id == categoryId) &&
                (query == null || event.title.contains(query, ignoreCase = true) ||
                    event.summary.contains(query, ignoreCase = true) ||
                    event.description?.contains(query, ignoreCase = true) == true)
        }
        return CatalogEvents.Cached(
            matches,
            events.map(Event::category).distinctBy(Category::id),
            Instant.ofEpochMilli(snapshot.downloadedAtMillis)
        )
    }

    override suspend fun getEvent(id: Long): Event = try {
        api.getEvent(id).toDomain()
    } catch (error: IOException) {
        findSavedEvent(id) ?: throw error
    } catch (error: HttpException) {
        when (error.code()) {
            404 -> favorites.findFavorite(id)
            in 500..599 -> findSavedEvent(id)
            else -> null
        } ?: throw error
    }

    private suspend fun findSavedEvent(id: Long): Event? =
        favorites.findFavorite(id) ?: cache.find(id)?.details?.toDomain(id)

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
