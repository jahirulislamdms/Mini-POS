package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Audit row for every stock change (BUILD_PLAN §9 — stock-movement history). */
@Serializable
@Entity(
    tableName = "stock_movements",
    indices = [Index("shopId"), Index("productId"), Index("createdAt")],
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val productId: Long,
    val change: Double,             // positive = stock in, negative = stock out
    val type: MovementType,
    val refId: Long? = null,        // id of the source Sale/Purchase, if any
    val note: String? = null,
    val createdAt: Long,
)
