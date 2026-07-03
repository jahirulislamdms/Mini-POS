package com.minipos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.minipos.data.entity.ActivityUndo
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityUndoDao {

    @Insert suspend fun insert(undo: ActivityUndo): Long

    @Query("SELECT * FROM activity_undos WHERE shopId = :shopId AND createdAt >= :since ORDER BY createdAt DESC")
    fun observeSince(shopId: Long, since: Long): Flow<List<ActivityUndo>>

    @Query("DELETE FROM activity_undos WHERE shopId = :shopId")
    suspend fun deleteForShop(shopId: Long)
}
