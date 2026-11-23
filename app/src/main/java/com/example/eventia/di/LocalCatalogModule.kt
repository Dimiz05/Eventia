package com.example.eventia.di

import android.content.Context
import androidx.room.Room
import com.example.eventia.data.local.EventiaDatabase
import com.example.eventia.data.local.FavoriteDao
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
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): EventiaDatabase =
            Room.databaseBuilder(context, EventiaDatabase::class.java, "eventia.db").build()

        @Provides
        fun provideFavoriteDao(database: EventiaDatabase): FavoriteDao = database.favoriteDao()
    }
}
