package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A manual cash adjustment (Cash In / Cash Out) not related to a sale or purchase.
 * Affects the dashboard Current Balance only — never recorded as a Sale/Purchase. Added in DB v3.
 */
@Serializable
@Entity(
    tableName = "cash_transactions",
    indices = [Index("shopId"), Index("createdAt")],
)
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val amount: Long,        // paisa, always positive
    val type: CashType,
    val note: String? = null,
    val createdAt: Long,
)
