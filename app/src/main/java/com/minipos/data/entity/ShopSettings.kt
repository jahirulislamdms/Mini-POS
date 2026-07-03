package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** One settings row per shop (currency label, low-stock default, notification prefs). CONVENTIONS §11. */
@Serializable
@Entity(
    tableName = "shop_settings",
    indices = [Index(value = ["shopId"], unique = true)],
)
data class ShopSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val currencyLabel: String = "৳",
    val lowStockDefault: Double = 5.0,
    val lowStockNotify: Boolean = true,
    val dueNotify: Boolean = true,
    val defaultUnit: String = "pcs",   // pre-selected unit for new products (Phase 31)
)
