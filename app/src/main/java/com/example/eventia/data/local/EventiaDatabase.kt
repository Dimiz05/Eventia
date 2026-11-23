package com.example.eventia.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.eventia.domain.model.Category
import com.example.eventia.domain.model.Event
import com.example.eventia.domain.model.Venue
import java.time.OffsetDateTime
import kotlinx.coroutines.flow.Flow

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

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val id: Long, @Embedded val details: StoredEvent)

@Entity(tableName = "cached_events")
data class CachedEventEntity(@PrimaryKey val id: Long, @Embedded val details: StoredEvent)

@Entity(tableName = "catalog_download")
data class CatalogDownloadEntity(@PrimaryKey val id: Int, val downloadedAtMillis: Long)

data class CachedCatalogSnapshot(
    val events: List<CachedEventEntity>,
    val downloadedAtMillis: Long
)

@Dao
abstract class FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY startsAt, id")
    abstract fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE id = :id")
    abstract suspend fun find(id: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    protected abstract suspend fun delete(id: Long)

    @Transaction
    open suspend fun remove(id: Long): FavoriteEntity? {
        val favorite = find(id) ?: return null
        delete(id)
        return favorite
    }
}

@Dao
abstract class CatalogCacheDao {
    @Query("SELECT * FROM cached_events WHERE id = :id")
    abstract suspend fun find(id: Long): CachedEventEntity?

    @Query("SELECT * FROM cached_events ORDER BY startsAt, id")
    protected abstract suspend fun getAll(): List<CachedEventEntity>

    @Query("SELECT downloadedAtMillis FROM catalog_download WHERE id = 1")
    protected abstract suspend fun getDownloadTime(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun saveDownload(download: CatalogDownloadEntity)

    @Insert
    protected abstract suspend fun insertAll(events: List<CachedEventEntity>)

    @Query("DELETE FROM cached_events")
    protected abstract suspend fun clear()

    @Transaction
    open suspend fun getSnapshot(): CachedCatalogSnapshot? {
        val downloadedAtMillis = getDownloadTime() ?: return null
        return CachedCatalogSnapshot(getAll(), downloadedAtMillis)
    }

    @Transaction
    open suspend fun replaceAll(events: List<CachedEventEntity>, downloadedAtMillis: Long) {
        clear()
        insertAll(events)
        saveDownload(CatalogDownloadEntity(id = 1, downloadedAtMillis = downloadedAtMillis))
    }
}

@Database(
    entities = [FavoriteEntity::class, CachedEventEntity::class, CatalogDownloadEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EventiaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun catalogCacheDao(): CatalogCacheDao
}
