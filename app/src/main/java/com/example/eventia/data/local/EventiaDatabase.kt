package com.example.eventia.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.eventia.data.local.dao.CatalogCacheDao
import com.example.eventia.data.local.dao.FavoriteDao
import com.example.eventia.data.local.entity.CachedEventEntity
import com.example.eventia.data.local.entity.CatalogDownloadEntity
import com.example.eventia.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class, CachedEventEntity::class, CatalogDownloadEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EventiaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun catalogCacheDao(): CatalogCacheDao
}
