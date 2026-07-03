package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Inventory item. Prices are Long paisa; [stock] is a quantity (Double, so weight
 * units like kg work). Optional fields are gated by their *Enabled toggle (BUILD_PLAN §6.4).
 */
@Serializable
@Entity(
    tableName = "products",
    indices = [Index("shopId"), Index("categoryId"), Index("subCategoryId"), Index("barcode")],
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val name: String,
    val barcode: String? = null,    // unique per shop; auto-generated when absent (Phase 28)
    val sellPrice: Long = 0,        // paisa
    val buyPrice: Long = 0,         // paisa
    val stock: Double = 0.0,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val unit: String? = null,
    val photoPath: String? = null,  // relative: shop_<id>/products/<uuid>.jpg
    val lowStockThreshold: Double? = null,  // null -> use ShopSettings.lowStockDefault
    val lowStockAlertEnabled: Boolean = true,
    val vatEnabled: Boolean = false,
    val vatPercent: Double = 0.0,
    val warrantyEnabled: Boolean = false,
    val warrantyText: String? = null,
    val wholesaleEnabled: Boolean = false,
    val wholesalePrice: Long? = null,   // paisa
    val discountEnabled: Boolean = false,
    val discountPercent: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
)
