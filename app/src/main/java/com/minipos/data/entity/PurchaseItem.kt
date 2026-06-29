package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A line within a purchase (mirror of SaleItem). [unitPrice] is the buy price snapshot. */
@Serializable
@Entity(
    tableName = "purchase_items",
    indices = [Index("shopId"), Index("purchaseId"), Index("productId")],
)
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val purchaseId: Long,
    val productId: Long? = null,
    val name: String,
    val unitPrice: Long = 0,        // paisa, buy price
    val quantity: Double = 0.0,
    val discount: Long = 0,         // paisa
    val lineTotal: Long = 0,        // paisa
)
