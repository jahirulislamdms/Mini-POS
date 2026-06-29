package com.minipos.feature.purchaseledger

import androidx.lifecycle.ViewModel
import com.minipos.ServiceLocator
import com.minipos.data.entity.Party
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.PurchaseItem

data class PurchaseDetailData(val purchase: Purchase, val items: List<PurchaseItem>, val party: Party?)

class PurchaseDetailViewModel : ViewModel() {

    private val purchaseRepo = ServiceLocator.purchaseRepository
    private val partyRepo = ServiceLocator.partyRepository

    suspend fun load(purchaseId: Long): PurchaseDetailData? {
        val purchase = purchaseRepo.getById(purchaseId) ?: return null
        val items = purchaseRepo.getItems(purchaseId)
        val party = purchase.partyId?.let { partyRepo.getParty(it) }
        return PurchaseDetailData(purchase, items, party)
    }
}
