package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A sale (quick or itemised). Cash sales settle in full; due sales leave [dueAmount]. */
@Serializable
@Entity(
    tableName = "sales",
    indices = [Index("shopId"), Index("partyId"), Index("createdAt")],
)
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val subtotal: Long = 0,         // paisa, before discount
    val discount: Long = 0,         // paisa, order-level discount
    val total: Long = 0,            // paisa, payable
    val paymentType: PaymentType = PaymentType.CASH,
    val partyId: Long? = null,      // set when paymentType = DUE
    val paidAmount: Long = 0,       // paisa paid now
    val dueAmount: Long = 0,        // paisa still owed
    val isQuickSale: Boolean = false,
    val note: String? = null,
    val createdAt: Long,
)
