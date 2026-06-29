package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A custom measurement unit (e.g. "pcs", "kg"). Named MeasureUnit to avoid clashing with
 * kotlin.Unit. Fully custom & editable per shop (BUILD_PLAN §3). Added in DB v2.
 */
@Serializable
@Entity(
    tableName = "units",
    indices = [Index("shopId")],
)
data class MeasureUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val name: String,
    val createdAt: Long,
)
