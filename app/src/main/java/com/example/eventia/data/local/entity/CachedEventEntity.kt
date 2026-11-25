package com.example.eventia.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.eventia.data.local.model.StoredEvent

@Entity(
    tableName = "cached_events"
)
data class CachedEventEntity(
    @PrimaryKey val id: Long,
    @Embedded val details: StoredEvent
)
