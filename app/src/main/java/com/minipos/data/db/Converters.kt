package com.minipos.data.db

import androidx.room.TypeConverter
import com.minipos.data.entity.CashType
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.PaymentDirection
import com.minipos.data.entity.PartyType
import com.minipos.data.entity.PaymentType

/** Enums are stored as their String name (CONVENTIONS §8). */
class Converters {
    @TypeConverter fun fromPaymentType(v: PaymentType): String = v.name
    @TypeConverter fun toPaymentType(v: String): PaymentType = PaymentType.valueOf(v)

    @TypeConverter fun fromPartyType(v: PartyType): String = v.name
    @TypeConverter fun toPartyType(v: String): PartyType = PartyType.valueOf(v)

    @TypeConverter fun fromDueDirection(v: DueDirection): String = v.name
    @TypeConverter fun toDueDirection(v: String): DueDirection = DueDirection.valueOf(v)

    @TypeConverter fun fromPaymentDirection(v: PaymentDirection): String = v.name
    @TypeConverter fun toPaymentDirection(v: String): PaymentDirection = PaymentDirection.valueOf(v)

    @TypeConverter fun fromMovementType(v: MovementType): String = v.name
    @TypeConverter fun toMovementType(v: String): MovementType = MovementType.valueOf(v)

    @TypeConverter fun fromCashType(v: CashType): String = v.name
    @TypeConverter fun toCashType(v: String): CashType = CashType.valueOf(v)
}
