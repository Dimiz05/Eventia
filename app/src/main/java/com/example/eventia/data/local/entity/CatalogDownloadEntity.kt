package com.example.eventia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalog_download"
)
data class CatalogDownloadEntity(
    @PrimaryKey val id: Int,
    val downloadedAtMillis: Long
)
