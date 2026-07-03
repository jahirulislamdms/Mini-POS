package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Audit record of an undo action (Phase 15). Inserted whenever a transaction is undone, so the
 * Activities list keeps a complete trail. The original transaction's rows are reversed + removed,
 * so this is what remains to show "X was undone".
 */
@Serializable
@Entity(
    tableName = "activity_undos",
    indices = [Index("shopId"), Index("createdAt")],
)
data class ActivityUndo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val targetType: String,     // the undone activity's type (SELL / BUY / EXPENSE / …)
    val targetRefId: Long,      // id of the original row that was undone
    val description: String,    // human-readable summary for the activity list
    val createdAt: Long,
)
