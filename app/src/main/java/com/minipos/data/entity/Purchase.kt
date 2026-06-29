package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A purchase from a supplier (mirror of Sale). Increments stock. */
@Serializable
@Entity(
    tableName = "purchases",
    indices = [Index("shopId"), Index("partyId"), Index("createdAt")],
)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val subtotal: Long = 0,         // paisa
    val discount: Long = 0,         // paisa
    val total: Long = 0,            // paisa
    val paymentType: PaymentType = PaymentType.CASH,
    val partyId: Long? = null,      // supplier, set when paymentType = DUE
    val paidAmount: Long = 0,       // paisa
    val dueAmount: Long = 0,        // paisa
    val note: String? = null,
    val createdAt: Long,
)
