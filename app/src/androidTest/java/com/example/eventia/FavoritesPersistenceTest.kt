package com.example.eventia

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventia.data.local.CachedEventEntity
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.StoredEvent
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.repositories.RoomFavoritesRepository
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesPersistenceTest {
    @Test
    fun replacingCatalogAndUndoingRemovalKeepTheSavedDetail() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, EventiaDatabase::class.java).build()
        try {
            withTimeout(15_000) {
                val cache = database.catalogCacheDao()
                val favorites = RoomFavoritesRepository(database.favoriteDao())
                // Reuse the existing CatalogMappingTest event.
                val event = Event(
                    id = 14,
                    title = "Compose",
                    summary = "Android",
                    description = null,
                    startsAt = OffsetDateTime.parse("2026-11-22T18:00:00-04:00"),
                    endsAt = null,
                    category = Category(7, "Tecnología"),
                    imageUrl = null,
                    venue = Venue(10, "Espacio Maker", "Calle Tadeo", latitude = -17.38),
                    capacity = 35,
                    availableSpots = 0,
                    isAttending = null
                )

                val cached = CachedEventEntity(event.id, StoredEvent.from(event))
                cache.replaceAll(listOf(cached))
                assertEquals(listOf(cached), cache.getAll())
                favorites.addFavorite(event)
                cache.replaceAll(emptyList())

                assertTrue(cache.getAll().isEmpty())
                assertEquals(listOf(event), favorites.observeFavorites().first())
                val removed = requireNotNull(favorites.removeFavorite(event.id))
                assertEquals(event, removed)
                assertTrue(favorites.observeFavorites().first().isEmpty())
                assertNull(favorites.findFavorite(event.id))

                favorites.addFavorite(removed)
                assertEquals(listOf(event), favorites.observeFavorites().first())
                assertEquals(event, favorites.findFavorite(event.id))
            }
        } finally {
            database.close()
        }
    }
}
