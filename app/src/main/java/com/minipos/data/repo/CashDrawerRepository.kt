package com.minipos.data.repo

import com.minipos.core.util.DateUtil
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.CashDrawerOpening
import com.minipos.data.entity.CashType
import com.minipos.data.entity.PaymentDirection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * One business day of the Cash Drawer (Phase 27). Everything except [opening] is derived from
 * the existing tables. Buying and supplier payments are deliberately excluded — the drawer only
 * tracks actual cash movement.
 */
data class CashDrawerDay(
    val dayStart: Long,
    val opening: Long,
    val cashSales: Long,
    val dueCollections: Long,
    val cashIn: Long,
    val cashOut: Long,
    val expenses: Long,
) {
    /** Net cash movement of the day (without the opening). */
    val net: Long
        get() = cashSales + dueCollections + cashIn - cashOut - expenses

    /** Closing = Opening + Cash Sales + Due Collections + Cash In − Cash Out − Expenses. */
    val closing: Long
        get() = opening + net
}

/** One cash-related transaction of a day (Phase 27.1). [amount] is signed (+ in / − out). */
data class DrawerTxn(
    val key: String,
    val createdAt: Long,
    val type: String,
    val description: String?,
    val amount: Long,
)

/**
 * Cash Drawer data (Phase 27/27.1). The Opening Cash is entered manually only on the very first
 * day; from then on it is **carried forward automatically** — Opening(day) = base manual opening
 * + net cash flow of all days in between (which equals the previous day's Closing). All daily
 * totals come live from the existing sales / due-payment / cash / expense data (no duplicated
 * records). Fully independent from the Current Balance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CashDrawerRepository(db: MiniPosDatabase) {

    private val drawerDao = db.cashDrawerDao()
    private val saleDao = db.saleDao()
    private val partyDao = db.partyDao()
    private val cashDao = db.cashTransactionDao()
    private val expenseDao = db.expenseDao()

    /** All manual opening rows (at most one in normal use — the first day's). */
    fun observeAllOpenings(shopId: Long): Flow<List<CashDrawerOpening>> =
        drawerDao.observeBetween(shopId, 0, Long.MAX_VALUE)

    /** Net drawer cash flow in [start]..[end] (0 for an empty range). */
    private fun netFlow(shopId: Long, start: Long, end: Long): Flow<Long> =
        if (end < start) {
            flowOf(0L)
        } else {
            combine(
                saleDao.observeCashInBetween(shopId, start, end),
                partyDao.observePaymentTotalBetween(shopId, PaymentDirection.RECEIVED, start, end),
                cashDao.observeBetween(shopId, start, end),
                expenseDao.observeTotalBetween(shopId, start, end),
            ) { cashSales, collections, cashTx, expenses ->
                val cashIn = cashTx.filter { it.type == CashType.CASH_IN }.sumOf { it.amount }
                val cashOut = cashTx.filter { it.type == CashType.CASH_OUT }.sumOf { it.amount }
                cashSales + collections + cashIn - cashOut - expenses
            }
        }

    /** The day's five cash components (opening left at 0 — filled in by the callers). */
    private fun components(shopId: Long, dayStart: Long, dayEnd: Long): Flow<CashDrawerDay> = combine(
        saleDao.observeCashInBetween(shopId, dayStart, dayEnd),
        partyDao.observePaymentTotalBetween(shopId, PaymentDirection.RECEIVED, dayStart, dayEnd),
        cashDao.observeBetween(shopId, dayStart, dayEnd),
        expenseDao.observeTotalBetween(shopId, dayStart, dayEnd),
    ) { cashSales, collections, cashTx, expenses ->
        CashDrawerDay(
            dayStart = dayStart,
            opening = 0,
            cashSales = cashSales,
            dueCollections = collections,
            cashIn = cashTx.filter { it.type == CashType.CASH_IN }.sumOf { it.amount },
            cashOut = cashTx.filter { it.type == CashType.CASH_OUT }.sumOf { it.amount },
            expenses = expenses,
        )
    }

    /**
     * Live drawer for a single day. Opening = the manual base (if it is this very day) or the
     * base amount plus all net cash flow since — i.e. yesterday's closing, carried forward.
     */
    fun observeDay(shopId: Long, dayStart: Long, dayEnd: Long): Flow<CashDrawerDay> =
        drawerDao.observeBetween(shopId, 0, dayStart).flatMapLatest { rows ->
            val base = rows.maxByOrNull { it.dayStart }
            val baseAmount = base?.amount ?: 0L
            val netStart = when {
                base == null -> 0L
                base.dayStart == dayStart -> dayStart // manual for this day: nothing carried
                else -> base.dayStart
            }
            combine(
                netFlow(shopId, netStart, dayStart - 1),
                components(shopId, dayStart, dayEnd),
            ) { carried, day ->
                day.copy(opening = baseAmount + carried)
            }
        }

    /**
     * Per-day drawer records inside [start]..[end], newest first, with openings chained from the
     * previous day's closing (seeded by the manual base before the range).
     */
    fun observeHistory(shopId: Long, start: Long, end: Long): Flow<List<CashDrawerDay>> =
        drawerDao.observeBetween(shopId, 0, end).flatMapLatest { rows ->
            val base = rows.filter { it.dayStart <= start }.maxByOrNull { it.dayStart }
            val baseAmount = base?.amount ?: 0L
            val netStart = when {
                base == null -> 0L
                base.dayStart == start -> start
                else -> base.dayStart
            }
            val manualInRange = rows.filter { it.dayStart in start..end }.associateBy { it.dayStart }
            combine(
                netFlow(shopId, netStart, start - 1),
                perDayComponents(shopId, start, end),
            ) { carriedIn, days ->
                val byDay = days.associateBy { it.dayStart }
                val allDays = (byDay.keys + manualInRange.keys).sorted()
                var carried = baseAmount + carriedIn
                allDays.map { dayStart ->
                    val comp = byDay[dayStart] ?: CashDrawerDay(dayStart, 0, 0, 0, 0, 0, 0)
                    val withOpening = comp.copy(
                        opening = manualInRange[dayStart]?.amount ?: carried,
                    )
                    carried = withOpening.closing
                    withOpening
                }.sortedByDescending { it.dayStart }
            }
        }

    /** Raw per-day components in a range (opening 0), from the existing transaction tables. */
    private fun perDayComponents(shopId: Long, start: Long, end: Long): Flow<List<CashDrawerDay>> = combine(
        saleDao.observeBetween(shopId, start, end),
        partyDao.observePayments(shopId),
        cashDao.observeBetween(shopId, start, end),
        expenseDao.observeBetween(shopId, start, end),
    ) { sales, allPayments, cashTx, expenses ->
        val received = allPayments.filter {
            it.direction == PaymentDirection.RECEIVED && it.createdAt in start..end
        }
        val salesByDay = sales.groupBy { DateUtil.startOfDay(it.createdAt) }
        val recvByDay = received.groupBy { DateUtil.startOfDay(it.createdAt) }
        val cashByDay = cashTx.groupBy { DateUtil.startOfDay(it.createdAt) }
        val expByDay = expenses.groupBy { DateUtil.startOfDay(it.createdAt) }

        (salesByDay.keys + recvByDay.keys + cashByDay.keys + expByDay.keys).map { day ->
            val dayCash = cashByDay[day].orEmpty()
            CashDrawerDay(
                dayStart = day,
                opening = 0,
                cashSales = salesByDay[day].orEmpty().sumOf { it.paidAmount },
                dueCollections = recvByDay[day].orEmpty().sumOf { it.amount },
                cashIn = dayCash.filter { it.type == CashType.CASH_IN }.sumOf { it.amount },
                cashOut = dayCash.filter { it.type == CashType.CASH_OUT }.sumOf { it.amount },
                expenses = expByDay[day].orEmpty().sumOf { it.amount },
            )
        }
    }

    /**
     * All cash-related transactions of a day (Phase 27.1), oldest first, signed amounts.
     * Cash sales, customer due collections, cash in/out and expenses only — never purchases
     * or supplier payments.
     */
    fun observeDayTransactions(shopId: Long, start: Long, end: Long): Flow<List<DrawerTxn>> = combine(
        saleDao.observeBetween(shopId, start, end),
        partyDao.observePayments(shopId),
        partyDao.observeParties(shopId),
        cashDao.observeBetween(shopId, start, end),
        expenseDao.observeBetween(shopId, start, end),
    ) { sales, allPayments, parties, cashTx, expenses ->
        val names = parties.associateBy({ it.id }, { it.name })
        buildList {
            sales.filter { it.paidAmount > 0 }.forEach { s ->
                val desc = s.note ?: if (s.isQuickSale) "Quick sale" else null
                add(DrawerTxn("sale_${s.id}", s.createdAt, "Cash Sale", desc, s.paidAmount))
            }
            allPayments
                .filter { it.direction == PaymentDirection.RECEIVED && it.createdAt in start..end }
                .forEach { p ->
                    add(DrawerTxn("pay_${p.id}", p.createdAt, "Due Collection", names[p.partyId], p.amount))
                }
            cashTx.forEach { c ->
                val isIn = c.type == CashType.CASH_IN
                add(
                    DrawerTxn(
                        "cash_${c.id}", c.createdAt,
                        if (isIn) "Cash In" else "Cash Out",
                        c.note,
                        if (isIn) c.amount else -c.amount,
                    ),
                )
            }
            expenses.forEach { e ->
                add(DrawerTxn("exp_${e.id}", e.createdAt, "Expense", e.note, -e.amount))
            }
        }.sortedBy { it.createdAt }
    }

    /** Enter (or edit) the Opening Cash — first-day seed only; later days carry forward. */
    suspend fun setOpening(shopId: Long, dayStart: Long, amount: Long) =
        drawerDao.upsert(
            CashDrawerOpening(
                shopId = shopId,
                dayStart = dayStart,
                amount = amount,
                updatedAt = System.currentTimeMillis(),
            ),
        )
}
