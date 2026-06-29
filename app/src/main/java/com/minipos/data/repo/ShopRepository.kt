package com.minipos.data.repo

import androidx.room.withTransaction
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.ExpenseCategory
import com.minipos.data.entity.MeasureUnit
import com.minipos.data.entity.Shop
import com.minipos.data.entity.ShopSettings
import kotlinx.coroutines.flow.Flow

/**
 * Shops + per-shop settings. Owns first-run seeding (CONVENTIONS §3): creating a shop also
 * creates its ShopSettings row and default (editable) expense categories, all in one transaction.
 */
class ShopRepository(private val db: MiniPosDatabase) {

    private val shopDao = db.shopDao()
    private val expenseDao = db.expenseDao()
    private val unitDao = db.measureUnitDao()

    fun observeShops(): Flow<List<Shop>> = shopDao.observeShops()
    fun observeShop(shopId: Long): Flow<Shop?> = shopDao.observeShop(shopId)
    suspend fun getShop(shopId: Long): Shop? = shopDao.getShop(shopId)
    suspend fun shopCount(): Int = shopDao.shopCount()

    fun observeSettings(shopId: Long): Flow<ShopSettings?> = shopDao.observeSettings(shopId)
    suspend fun getSettings(shopId: Long): ShopSettings? = shopDao.getSettings(shopId)
    suspend fun updateSettings(settings: ShopSettings) = shopDao.updateSettings(settings)
    suspend fun updateShop(shop: Shop) = shopDao.updateShop(shop)

    /** Create a shop together with its settings and default expense categories. Returns the new shopId. */
    suspend fun createShop(
        name: String,
        address: String? = null,
        phone: String? = null,
        currencyLabel: String = "৳",
        lowStockDefault: Double = 5.0,
    ): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val shopId = shopDao.insertShop(
            Shop(name = name, address = address, phone = phone, createdAt = now),
        )
        shopDao.insertSettings(
            ShopSettings(
                shopId = shopId,
                currencyLabel = currencyLabel,
                lowStockDefault = lowStockDefault,
            ),
        )
        expenseDao.insertCategories(
            DEFAULT_EXPENSE_CATEGORIES.map { ExpenseCategory(shopId = shopId, name = it, createdAt = now) },
        )
        unitDao.insertAll(
            DEFAULT_UNITS.map { MeasureUnit(shopId = shopId, name = it, createdAt = now) },
        )
        shopId
    }

    /** Delete a shop and every row scoped to it (CONVENTIONS §3 — removes only that shop's data). */
    suspend fun deleteShop(shopId: Long) = db.withTransaction {
        db.productDao().deleteForShop(shopId)
        db.stockMovementDao().deleteForShop(shopId)
        db.categoryDao().deleteForShop(shopId)
        db.saleDao().deleteSalesForShop(shopId)
        db.saleDao().deleteItemsForShop(shopId)
        db.purchaseDao().deletePurchasesForShop(shopId)
        db.purchaseDao().deleteItemsForShop(shopId)
        db.expenseDao().deleteExpensesForShop(shopId)
        db.expenseDao().deleteCategoriesForShop(shopId)
        db.measureUnitDao().deleteForShop(shopId)
        db.cashTransactionDao().deleteForShop(shopId)
        db.partyDao().deletePartiesForShop(shopId)
        db.partyDao().deleteDuesForShop(shopId)
        db.partyDao().deletePaymentsForShop(shopId)
        shopDao.deleteSettingsForShop(shopId)
        shopDao.deleteShopById(shopId)
    }

    companion object {
        /** Editable in Settings (P7/P12); these are just the starting set. */
        val DEFAULT_EXPENSE_CATEGORIES = listOf("Salary", "Rent", "Bill", "Purchase")

        /** Starter units; all editable/deletable (BUILD_PLAN §3). */
        val DEFAULT_UNITS = listOf("pcs", "kg", "g", "litre", "ml", "dozen", "packet")
    }
}
