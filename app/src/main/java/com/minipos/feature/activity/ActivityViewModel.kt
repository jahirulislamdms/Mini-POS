package com.minipos.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.Money
import com.minipos.data.entity.CashType
import com.minipos.data.entity.PaymentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class ActivityType { SELL, BUY, EXPENSE, CASH_IN, CASH_OUT, STOCK_ADJUSTMENT, UNDO }

/** One row in the Activities list (a transaction or an undo audit entry). */
data class ActivityItem(
    val key: String,
    val type: ActivityType,
    val refId: Long,
    val title: String,
    val subtitle: String?,
    val amountText: String?,
    val createdAt: Long,
    val undoable: Boolean,
)

/**
 * Activities & Undo (Phase 15). Aggregates the last 30 days of sells, buys, expenses, cash in/out,
 * stock adjustments and undo records into one chronological list. Undo reverses + removes the
 * original transaction and logs an audit entry. List is filter-only (no old data deleted).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel : ViewModel() {

    private val repo = ServiceLocator.activityRepository
    private val productRepo = ServiceLocator.productRepository
    private val partyRepo = ServiceLocator.partyRepository
    private val expenseRepo = ServiceLocator.expenseRepository

    private val shopIdState = MutableStateFlow<Long?>(null)
    fun setShop(shopId: Long) { shopIdState.value = shopId }

    private fun windowStart() = System.currentTimeMillis() - WINDOW_MILLIS

    private val salesF = shopIdState.filterNotNull().flatMapLatest { repo.observeSalesSince(it, windowStart()) }
    private val purchasesF = shopIdState.filterNotNull().flatMapLatest { repo.observePurchasesSince(it, windowStart()) }
    private val expensesF = shopIdState.filterNotNull().flatMapLatest { repo.observeExpensesSince(it, windowStart()) }
    private val cashF = shopIdState.filterNotNull().flatMapLatest { repo.observeCashSince(it, windowStart()) }
    private val adjustmentsF = shopIdState.filterNotNull().flatMapLatest { repo.observeAdjustmentsSince(it, windowStart()) }
    private val undosF = shopIdState.filterNotNull().flatMapLatest { repo.observeUndosSince(it, windowStart()) }
    private val productsF = shopIdState.filterNotNull().flatMapLatest { productRepo.observeByShop(it) }
    private val partiesF = shopIdState.filterNotNull().flatMapLatest { partyRepo.observeParties(it) }
    private val categoriesF = shopIdState.filterNotNull().flatMapLatest { expenseRepo.observeCategories(it) }

    val activities: StateFlow<List<ActivityItem>> = combine(
        combine(salesF, purchasesF, expensesF) { s, p, e -> Triple(s, p, e) },
        combine(cashF, adjustmentsF, undosF) { c, a, u -> Triple(c, a, u) },
        combine(productsF, partiesF, categoriesF) { pr, pa, ca -> Triple(pr, pa, ca) },
    ) { txn, other, lookups ->
        buildActivities(txn, other, lookups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildActivities(
        txn: Triple<List<com.minipos.data.entity.Sale>, List<com.minipos.data.entity.Purchase>, List<com.minipos.data.entity.Expense>>,
        other: Triple<List<com.minipos.data.entity.CashTransaction>, List<com.minipos.data.entity.StockMovement>, List<com.minipos.data.entity.ActivityUndo>>,
        lookups: Triple<List<com.minipos.data.entity.Product>, List<com.minipos.data.entity.Party>, List<com.minipos.data.entity.ExpenseCategory>>,
    ): List<ActivityItem> {
        val (sales, purchases, expenses) = txn
        val (cash, adjustments, undos) = other
        val (products, parties, categories) = lookups

        val productNames = products.associateBy({ it.id }, { it.name })
        val partyNames = parties.associateBy({ it.id }, { it.name })
        val categoryNames = categories.associateBy({ it.id }, { it.name })

        val items = ArrayList<ActivityItem>()

        sales.forEach { s ->
            val party = s.partyId?.let { partyNames[it] }
            val sub = buildString {
                append(if (s.paymentType == PaymentType.DUE) "Due" else "Cash")
                if (party != null) append(" · ").append(party)
                if (s.isQuickSale) append(" · Quick")
            }
            items += ActivityItem("sell_${s.id}", ActivityType.SELL, s.id, "Sale", sub, Money.format(s.total), s.createdAt, true)
        }
        purchases.forEach { p ->
            val party = p.partyId?.let { partyNames[it] }
            val sub = buildString {
                append(if (p.paymentType == PaymentType.DUE) "Due" else "Cash")
                if (party != null) append(" · ").append(party)
            }
            items += ActivityItem("buy_${p.id}", ActivityType.BUY, p.id, "Purchase", sub, Money.format(p.total), p.createdAt, true)
        }
        expenses.forEach { e ->
            val sub = listOfNotNull(e.categoryId?.let { categoryNames[it] }, e.note)
                .joinToString(" · ").ifBlank { null }
            items += ActivityItem("exp_${e.id}", ActivityType.EXPENSE, e.id, "Expense", sub, Money.format(e.amount), e.createdAt, true)
        }
        cash.forEach { c ->
            val isIn = c.type == CashType.CASH_IN
            items += ActivityItem(
                "cash_${c.id}",
                if (isIn) ActivityType.CASH_IN else ActivityType.CASH_OUT,
                c.id,
                if (isIn) "Cash In" else "Cash Out",
                c.note,
                Money.format(c.amount),
                c.createdAt,
                true,
            )
        }
        adjustments.forEach { m ->
            val name = productNames[m.productId] ?: "(deleted product)"
            val qty = (if (m.change >= 0) "+" else "−") + abs(m.change).asQty()
            val sub = listOfNotNull("$name  $qty", m.note).joinToString(" · ")
            items += ActivityItem("adj_${m.id}", ActivityType.STOCK_ADJUSTMENT, m.id, "Stock adjustment", sub, null, m.createdAt, true)
        }
        undos.forEach { u ->
            items += ActivityItem("undo_${u.id}", ActivityType.UNDO, u.targetRefId, u.description, "Undone", null, u.createdAt, false)
        }

        return items.sortedByDescending { it.createdAt }
    }

    /** Reverse + remove the underlying transaction and log an undo audit entry. */
    fun undo(item: ActivityItem) = viewModelScope.launch {
        val shopId = shopIdState.value ?: return@launch
        val description = buildString {
            append("Undid ").append(item.title.lowercase())
            item.amountText?.let { append(" ").append(it) }
        }
        when (item.type) {
            ActivityType.SELL -> repo.undoSale(shopId, item.refId, description)
            ActivityType.BUY -> repo.undoPurchase(shopId, item.refId, description)
            ActivityType.EXPENSE -> repo.undoExpense(shopId, item.refId, description)
            ActivityType.CASH_IN -> repo.undoCash(shopId, item.refId, "CASH_IN", description)
            ActivityType.CASH_OUT -> repo.undoCash(shopId, item.refId, "CASH_OUT", description)
            ActivityType.STOCK_ADJUSTMENT -> repo.undoStockAdjustment(shopId, item.refId, description)
            ActivityType.UNDO -> Unit // an undo entry is itself not undoable
        }
    }

    private fun Double.asQty(): String =
        if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

    companion object {
        /** Show / allow undo for the last 30 days only. */
        const val WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
