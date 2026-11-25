package com.example.eventia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.eventia.data.local.entity.CachedEventEntity
import com.example.eventia.data.local.entity.CatalogDownloadEntity
import com.example.eventia.data.local.model.CachedCatalogSnapshot

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
