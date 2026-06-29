package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A shop. The Shop row itself is the top of each shop's data tree (no shopId on itself). */
@Serializable
@Entity(tableName = "shops")
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val logoPath: String? = null,   // relative: shop_<id>/logo.jpg (CONVENTIONS §11)
    val address: String? = null,
    val phone: String? = null,
    val createdAt: Long,
)
