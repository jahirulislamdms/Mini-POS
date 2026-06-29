package com.minipos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A customer / supplier / employee. Parties live inside the Due ledger (BUILD_PLAN §6.8). */
@Serializable
@Entity(
    tableName = "parties",
    indices = [Index("shopId")],
)
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shopId: Long,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val type: PartyType = PartyType.CUSTOMER,
    val createdAt: Long,
)
