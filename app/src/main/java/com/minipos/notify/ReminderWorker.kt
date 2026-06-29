package com.minipos.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.minipos.ServiceLocator
import com.minipos.core.util.Money
import com.minipos.data.entity.DueDirection
import com.minipos.data.entity.PaymentDirection
import kotlinx.coroutines.flow.first

/**
 * Daily check for the CURRENT shop: posts a low-stock reminder and/or a "money to collect"
 * reminder, each gated by its ShopSettings toggle (P12.2).
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        ServiceLocator.init(context) // idempotent

        val shopId = ServiceLocator.currentShopManager.currentShopId.first() ?: return Result.success()
        val settings = ServiceLocator.shopRepository.getSettings(shopId)

        if (settings?.lowStockNotify != false) {
            val lowDefault = settings?.lowStockDefault ?: 5.0
            val low = ServiceLocator.productRepository.observeLowStock(shopId, lowDefault).first()
            if (low.isNotEmpty()) {
                Notifier.show(
                    context,
                    Notifier.ID_LOW_STOCK,
                    "Low stock",
                    "${low.size} product(s) need restocking.",
                )
            }
        }

        if (settings?.dueNotify != false) {
            val dues = ServiceLocator.partyRepository.observeDues(shopId).first()
            val payments = ServiceLocator.partyRepository.observePayments(shopId).first()
            val receivable =
                dues.filter { it.direction == DueDirection.RECEIVABLE }.sumOf { it.amount } -
                    payments.filter { it.direction == PaymentDirection.RECEIVED }.sumOf { it.amount }
            if (receivable > 0) {
                Notifier.show(
                    context,
                    Notifier.ID_DUE,
                    "Money to collect",
                    "You'll receive ${Money.format(receivable)} from customers.",
                )
            }
        }

        return Result.success()
    }
}
