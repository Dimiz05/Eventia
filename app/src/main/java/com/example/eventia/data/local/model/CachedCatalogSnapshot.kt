package com.example.eventia.data.local.model

import com.example.eventia.data.local.entity.CachedEventEntity

data class CachedCatalogSnapshot(
    val events: List<CachedEventEntity>,
    val downloadedAtMillis: Long
)
