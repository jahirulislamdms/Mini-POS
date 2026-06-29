package com.minipos.feature.shop

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minipos.ServiceLocator
import com.minipos.core.util.ImageStorage
import com.minipos.data.entity.Shop
import com.minipos.data.entity.ShopSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Loaded shop + settings for the edit form. */
data class ShopFormData(val shop: Shop, val settings: ShopSettings?)

/** Result of saving the shop form. Created flips the current-shop gate; Updated just closes. */
sealed interface SaveResult {
    data class Created(val id: Long) : SaveResult
    data object Updated : SaveResult
    data object NotFound : SaveResult
}

class ShopViewModel(app: Application) : AndroidViewModel(app) {

    private val shopRepo = ServiceLocator.shopRepository
    private val shopManager = ServiceLocator.currentShopManager

    val shops: StateFlow<List<Shop>> = shopRepo.observeShops()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentShopId: StateFlow<Long?> = shopManager.currentShopId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun switchTo(shopId: Long) = viewModelScope.launch {
        shopManager.setCurrentShop(shopId)
    }

    fun deleteShop(shop: Shop) = viewModelScope.launch {
        val wasCurrent = shopManager.currentShopId.first() == shop.id
        shopRepo.deleteShop(shop.id)
        ImageStorage.deleteShopFiles(getApplication(), shop.id)
        if (wasCurrent) {
            val remaining = shopRepo.observeShops().first()
            if (remaining.isEmpty()) shopManager.clear()
            else shopManager.setCurrentShop(remaining.first().id)
        }
    }

    suspend fun loadForEdit(shopId: Long): ShopFormData? {
        val shop = shopRepo.getShop(shopId) ?: return null
        return ShopFormData(shop, shopRepo.getSettings(shopId))
    }

    /** Create (and switch to) or update a shop + its settings, copying a newly picked logo if any. */
    suspend fun save(
        editingId: Long?,
        name: String,
        address: String?,
        phone: String?,
        currencyLabel: String,
        lowStockDefault: Double,
        logoUri: Uri?,
    ): SaveResult {
        val context = getApplication<Application>()
        return if (editingId == null) {
            val id = shopRepo.createShop(name, address, phone, currencyLabel, lowStockDefault)
            if (logoUri != null) {
                val path = ImageStorage.copyShopLogo(context, id, logoUri)
                shopRepo.getShop(id)?.let { shopRepo.updateShop(it.copy(logoPath = path)) }
            }
            shopManager.setCurrentShop(id)
            SaveResult.Created(id)
        } else {
            val existing = shopRepo.getShop(editingId) ?: return SaveResult.NotFound
            val logoPath = if (logoUri != null) {
                ImageStorage.copyShopLogo(context, editingId, logoUri)
            } else {
                existing.logoPath
            }
            shopRepo.updateShop(
                existing.copy(name = name, address = address, phone = phone, logoPath = logoPath),
            )
            shopRepo.getSettings(editingId)?.let {
                shopRepo.updateSettings(
                    it.copy(currencyLabel = currencyLabel, lowStockDefault = lowStockDefault),
                )
            }
            SaveResult.Updated
        }
    }
}
