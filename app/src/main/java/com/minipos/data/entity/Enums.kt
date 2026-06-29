package com.minipos.data.entity

import kotlinx.serialization.Serializable

/** How a sale/purchase was settled. */
@Serializable
enum class PaymentType { CASH, DUE }

/** Party grouping in the Due ledger (BUILD_PLAN §6.8). */
@Serializable
enum class PartyType { CUSTOMER, SUPPLIER, EMPLOYEE }

/** A due's side of the ledger: RECEIVABLE = "you'll receive", PAYABLE = "you'll give". */
@Serializable
enum class DueDirection { RECEIVABLE, PAYABLE }

/** A due payment's direction: money RECEIVED from / GIVEN to a party. */
@Serializable
enum class PaymentDirection { RECEIVED, GIVEN }

/** Why stock changed (for the stock-movement history). */
@Serializable
enum class MovementType { INITIAL, PURCHASE, SALE, ADJUSTMENT }

/** Manual cash movement not tied to a sale/purchase (affects Current Balance only). */
@Serializable
enum class CashType { CASH_IN, CASH_OUT }
