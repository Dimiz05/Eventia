package com.example.eventia

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eventia.api.EventiaApi
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.SessionStore
import com.example.eventia.data.local.entity.CachedEventEntity
import com.example.eventia.data.local.model.StoredEvent
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import com.example.eventia.repositories.CatalogEvents
import com.example.eventia.repositories.RemoteCatalogRepository
import com.example.eventia.repositories.RoomFavoritesRepository
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class FavoritesPersistenceTest {
    @Test
    fun replacingCatalogAndUndoingRemovalKeepTheSavedDetail() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = File.createTempFile("eventia-favorites-", ".db", context.cacheDir)
        fun openDatabase() = Room.databaseBuilder(
            context, EventiaDatabase::class.java, databaseFile.absolutePath
        ).build()
        var database = openDatabase()
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
                val downloadedAt = event.startsAt.toInstant()
                assertNull(cache.getSnapshot())

                val networkAvailable = AtomicBoolean(false)
                val api = Retrofit.Builder()
                    .baseUrl("https://example.invalid/")
                    .client(OkHttpClient.Builder().addInterceptor { chain ->
                        if (!networkAvailable.get()) throw IOException()
                        Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("[]".toResponseBody("application/json".toMediaType()))
                            .build()
                    }.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(EventiaApi::class.java)
                val catalog = RemoteCatalogRepository(api, favorites, cache, SessionStore(context))
                val firstFailure = runCatching { catalog.loadEvents(null, null) }.exceptionOrNull()
                assertTrue(firstFailure is IOException)
                assertNull(cache.getSnapshot())

                cache.replaceAll(listOf(cached), downloadedAt.toEpochMilli())
                val snapshot = requireNotNull(cache.getSnapshot())
                assertEquals(listOf(cached), snapshot.events)
                assertEquals(downloadedAt.toEpochMilli(), snapshot.downloadedAtMillis)

                networkAvailable.set(true)
                assertEquals(CatalogEvents.Remote(emptyList()), catalog.loadEvents(event.title, null))
                assertEquals(CatalogEvents.Remote(emptyList()), catalog.loadEvents(null, event.category.id))
                assertEquals(snapshot, cache.getSnapshot())

                networkAvailable.set(false)
                assertEquals(event, catalog.getEvent(event.id))
                val offline = CatalogEvents.Cached(listOf(event), listOf(event.category), downloadedAt)
                assertEquals(offline, catalog.loadEvents(event.title.lowercase(), event.category.id))
                assertEquals(offline.copy(events = emptyList()), catalog.loadEvents(event.venue.address, null))
                assertEquals(snapshot, cache.getSnapshot())

                favorites.addFavorite(event)
                assertEquals(offline, catalog.loadEvents(event.title, event.category.id))
                assertEquals(offline.copy(events = emptyList()), catalog.loadEvents(event.venue.address, null))
                assertEquals(listOf(event), favorites.observeFavorites().first())

                networkAvailable.set(true)
                val beforeDownload = System.currentTimeMillis()
                assertEquals(CatalogEvents.Remote(emptyList()), catalog.loadEvents(null, null))
                val emptyDownload = requireNotNull(cache.getSnapshot())

                assertTrue(emptyDownload.events.isEmpty())
                assertTrue(emptyDownload.downloadedAtMillis in beforeDownload..System.currentTimeMillis())
                networkAvailable.set(false)
                assertEquals(
                    CatalogEvents.Cached(
                        emptyList(),
                        emptyList(),
                        Instant.ofEpochMilli(emptyDownload.downloadedAtMillis)
                    ),
                    catalog.loadEvents(null, null)
                )
                assertEquals(listOf(event), favorites.observeFavorites().first())
                assertEquals(event, catalog.getEvent(event.id))
                val removed = requireNotNull(favorites.removeFavorite(event.id))
                assertEquals(event, removed)
                assertTrue(favorites.observeFavorites().first().isEmpty())
                assertNull(favorites.findFavorite(event.id))

                favorites.addFavorite(removed)
                assertEquals(listOf(event), favorites.observeFavorites().first())
                assertEquals(event, favorites.findFavorite(event.id))

                database.close()
                database = openDatabase()
                val reopenedFavorites = RoomFavoritesRepository(database.favoriteDao())
                val reopenedCatalog = RemoteCatalogRepository(
                    api, reopenedFavorites, database.catalogCacheDao(), SessionStore(context)
                )
                assertEquals(listOf(event), reopenedFavorites.observeFavorites().first())
                assertEquals(event, reopenedCatalog.getEvent(event.id))
            }
        } finally {
            database.close()
            context.deleteDatabase(databaseFile.absolutePath)
        }
    }
}
