package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Fully custom product category. A sub-category is just a Category whose [parentId]
 * points at its parent (BUILD_PLAN §3 — no fixed presets).
 */
@Serializable
@Entity(
    tableName = "categories",
    indices = [Index("shopId"), Index("parentId")],
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val name: String,
    val parentId: Long? = null,   // null = top-level category; else sub-category of parentId
    val createdAt: Long,
)
