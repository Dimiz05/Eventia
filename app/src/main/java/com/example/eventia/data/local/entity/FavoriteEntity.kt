package com.example.eventia.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.eventia.data.local.model.StoredEvent

@Entity(
    tableName = "favorites"
)
data class FavoriteEntity(
    @PrimaryKey val id: Long,
    @Embedded val details: StoredEvent)
