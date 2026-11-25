package com.example.eventia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.eventia.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY startsAt, id")
    abstract fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE id = :id")
    abstract suspend fun find(id: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    protected abstract suspend fun delete(id: Long)

    @Transaction
    open suspend fun remove(id: Long): FavoriteEntity? {
        val favorite = find(id) ?: return null
        delete(id)
        return favorite
    }
}
