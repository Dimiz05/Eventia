package com.example.eventia.repositories

import com.example.eventia.api.EventiaApi
import com.example.eventia.domain.model.Category
import javax.inject.Inject

interface CategoryRepository {
    suspend fun getCategories(): List<Category>
}

class RemoteCategoryRepository @Inject constructor(
    private val api: EventiaApi
) : CategoryRepository {
    override suspend fun getCategories(): List<Category> =
        api.getCategories().map { Category(id = it.id, name = it.name) }
}
