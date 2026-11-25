package com.example.eventia.repositories

import com.example.eventia.api.EventiaApi
import com.example.eventia.api.mappers.toDomain
import com.example.eventia.api.models.EventDto
import com.example.eventia.api.models.VenueDto
import com.example.eventia.data.local.SessionStore
import com.example.eventia.data.local.dao.CatalogCacheDao
import com.example.eventia.data.local.entity.CachedEventEntity
import com.example.eventia.data.local.model.StoredEvent
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import retrofit2.HttpException

class RemoteCatalogRepository @Inject constructor(
    private val api: EventiaApi,
    private val favorites: FavoritesRepository,
    private val cache: CatalogCacheDao,
    private val sessionStore: SessionStore
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

    override suspend fun getEvent(id: Long, authenticated: Boolean, allowSaved: Boolean): Event {
        val authorization = if (authenticated) {
            val token = sessionStore.readToken()?.takeIf(String::isNotBlank) ?: throw EventSessionExpired()
            "Bearer $token"
        } else null

        return try {
            api.getEvent(id, authorization).toDomain()
        } catch (error: IOException) {
            if (!allowSaved) throw error
            findSavedEvent(id) ?: throw error
        } catch (error: HttpException) {
            if (authenticated && error.code() == 401) throw EventSessionExpired()
            if (!allowSaved) throw error
            when (error.code()) {
                404 -> favorites.findFavorite(id)
                in 500..599 -> findSavedEvent(id)
                else -> null
            } ?: throw error
        }
    }

    private suspend fun findSavedEvent(id: Long): Event? =
        favorites.findFavorite(id) ?: cache.find(id)?.details?.toDomain(id)

    override suspend fun getVenues(): List<Venue> = api.getVenues().map(VenueDto::toDomain)
    override suspend fun getVenue(id: Long): Venue = api.getVenue(id).toDomain()
    override suspend fun getVenueEvents(id: Long): List<Event> =
        api.getVenueEvents(id).map(EventDto::toDomain)
}
