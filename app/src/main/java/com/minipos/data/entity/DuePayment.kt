package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A payment against a party's dues. [direction] = money RECEIVED from / GIVEN to the party. */
@Serializable
@Entity(
    tableName = "due_payments",
    indices = [Index("shopId"), Index("partyId"), Index("dueId"), Index("createdAt")],
)
data class DuePayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val partyId: Long,
    val dueId: Long? = null,        // optional: a specific Due this settles
    val amount: Long = 0,           // paisa
    val direction: PaymentDirection,
    val note: String? = null,
    val createdAt: Long,
)
