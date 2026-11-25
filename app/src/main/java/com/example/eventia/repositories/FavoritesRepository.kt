package com.example.eventia.repositories

import com.example.eventia.data.local.dao.FavoriteDao
import com.example.eventia.data.local.entity.FavoriteEntity
import com.example.eventia.data.local.model.StoredEvent
import com.example.eventia.domain.model.Event
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface FavoritesRepository {
    fun observeFavorites(): Flow<List<Event>>
    suspend fun findFavorite(id: Long): Event?
    suspend fun addFavorite(event: Event)
    suspend fun removeFavorite(id: Long): Event?
}

class RoomFavoritesRepository @Inject constructor(
    private val favorites: FavoriteDao
) : FavoritesRepository {
    override fun observeFavorites(): Flow<List<Event>> =
        favorites.observeAll().map { rows -> rows.map { it.details.toDomain(it.id) } }

    override suspend fun findFavorite(id: Long): Event? =
        favorites.find(id)?.let { it.details.toDomain(it.id) }

    override suspend fun addFavorite(event: Event) {
        favorites.insert(FavoriteEntity(event.id, StoredEvent.from(event)))
    }

    override suspend fun removeFavorite(id: Long): Event? =
        favorites.remove(id)?.let { it.details.toDomain(it.id) }
}
