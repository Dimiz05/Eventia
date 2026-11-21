package com.example.eventia.di

import com.example.eventia.api.EventiaApi
import com.example.eventia.repositories.CategoryRepository
import com.example.eventia.repositories.RemoteCategoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindCategoryRepository(repository: RemoteCategoryRepository): CategoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideEventiaApi(): EventiaApi = Retrofit.Builder()
            .baseUrl("https://eventia.jmacboy.dev/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventiaApi::class.java)
    }
}
