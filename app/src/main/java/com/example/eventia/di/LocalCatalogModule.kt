package com.example.eventia.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.dao.CatalogCacheDao
import com.example.eventia.data.local.dao.FavoriteDao
import com.example.eventia.repositories.FavoritesRepository
import com.example.eventia.repositories.RoomFavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalCatalogModule {
    @Binds
    abstract fun bindFavorites(repository: RoomFavoritesRepository): FavoritesRepository

    companion object {
        internal val catalogDownloadMigration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE catalog_download (id INTEGER NOT NULL PRIMARY KEY, downloadedAtMillis INTEGER NOT NULL)"
                )
            }
        }

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): EventiaDatabase =
            Room.databaseBuilder(context, EventiaDatabase::class.java, "eventia.db")
                .addMigrations(catalogDownloadMigration)
                .build()

        @Provides
        fun provideFavoriteDao(database: EventiaDatabase): FavoriteDao = database.favoriteDao()

        @Provides
        fun provideCatalogCacheDao(database: EventiaDatabase): CatalogCacheDao = database.catalogCacheDao()
    }
}
