package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * The user-entered Opening Cash for one business day (Phase 27 Cash Drawer). One row per shop
 * per day ([dayStart] = start-of-day millis); everything else in the drawer is derived live from
 * the existing sales / due-payment / cash / expense tables.
 */
@Serializable
@Entity(
    tableName = "cash_drawer_openings",
    indices = [Index("shopId"), Index(value = ["shopId", "dayStart"], unique = true)],
)
data class CashDrawerOpening(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val dayStart: Long,             // DateUtil.startOfDay millis of the day
    val amount: Long = 0,           // paisa
    val updatedAt: Long,
)
