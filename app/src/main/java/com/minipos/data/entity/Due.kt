package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A debt entry for a party. [direction] decides the side of the ledger
 * (RECEIVABLE = you'll receive, PAYABLE = you'll give). Running balances are
 * computed from Dues minus DuePayments in the Due ledger (P8).
 */
@Serializable
@Entity(
    tableName = "dues",
    indices = [Index("shopId"), Index("partyId"), Index("createdAt")],
)
data class Due(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val partyId: Long,
    val amount: Long = 0,           // paisa
    val direction: DueDirection,
    val refType: String? = null,    // "SALE" / "PURCHASE" / "MANUAL"
    val refId: Long? = null,        // id of the source Sale/Purchase, if any
    val note: String? = null,
    val createdAt: Long,
)
