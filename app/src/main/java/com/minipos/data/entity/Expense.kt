package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A business expense under a custom ExpenseCategory (BUILD_PLAN §6.7). */
@Serializable
@Entity(
    tableName = "expenses",
    indices = [Index("shopId"), Index("categoryId"), Index("createdAt")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val categoryId: Long? = null,
    val amount: Long = 0,           // paisa
    val note: String? = null,
    val createdAt: Long,
)
