package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A line within a sale. [name]/[unitPrice] are snapshots so history is stable if the product changes. */
@Serializable
@Entity(
    tableName = "sale_items",
    indices = [Index("shopId"), Index("saleId"), Index("productId")],
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val saleId: Long,
    val productId: Long? = null,    // null for a free-text / quick line
    val name: String,
    val unitPrice: Long = 0,        // paisa
    val quantity: Double = 0.0,
    val discount: Long = 0,         // paisa, per-item
    val lineTotal: Long = 0,        // paisa = unitPrice * quantity - discount
)
