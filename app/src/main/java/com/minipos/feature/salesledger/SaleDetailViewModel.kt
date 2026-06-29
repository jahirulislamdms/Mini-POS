package com.minipos.feature.salesledger

import androidx.lifecycle.ViewModel
import com.minipos.ServiceLocator
import com.minipos.data.entity.Party
import com.minipos.data.entity.Sale
import com.minipos.data.entity.SaleItem

data class SaleDetailData(val sale: Sale, val items: List<SaleItem>, val party: Party?)

class SaleDetailViewModel : ViewModel() {

    private val saleRepo = ServiceLocator.saleRepository
    private val partyRepo = ServiceLocator.partyRepository

    suspend fun load(saleId: Long): SaleDetailData? {
        val sale = saleRepo.getById(saleId) ?: return null
        val items = saleRepo.getItems(saleId)
        val party = sale.partyId?.let { partyRepo.getParty(it) }
        return SaleDetailData(sale, items, party)
    }
}
