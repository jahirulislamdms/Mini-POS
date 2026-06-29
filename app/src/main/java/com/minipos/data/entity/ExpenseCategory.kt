package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Custom expense category (BUILD_PLAN §6.11). Defaults seeded per shop, all editable. */
@Serializable
@Entity(
    tableName = "expense_categories",
    indices = [Index("shopId")],
)
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val name: String,
    val createdAt: Long,
)
