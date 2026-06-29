package com.minipos.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.minipos.data.dao.CashTransactionDao
import com.minipos.data.dao.CategoryDao
import com.minipos.data.dao.ExpenseDao
import com.minipos.data.dao.PartyDao
import com.minipos.data.dao.ProductDao
import com.minipos.data.dao.PurchaseDao
import com.minipos.data.dao.SaleDao
import com.minipos.data.dao.MeasureUnitDao
import com.minipos.data.dao.ShopDao
import com.minipos.data.dao.StockMovementDao
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.Category
import com.minipos.data.entity.Due
import com.minipos.data.entity.DuePayment
import com.minipos.data.entity.Expense
import com.minipos.data.entity.ExpenseCategory
import com.minipos.data.entity.MeasureUnit
import com.minipos.data.entity.Party
import com.minipos.data.entity.Product
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.PurchaseItem
import com.minipos.data.entity.Sale
import com.minipos.data.entity.SaleItem
import com.minipos.data.entity.Shop
import com.minipos.data.entity.ShopSettings
import com.minipos.data.entity.StockMovement

@Database(
    entities = [
        Shop::class,
        ShopSettings::class,
        Category::class,
        Product::class,
        StockMovement::class,
        Sale::class,
        SaleItem::class,
        Purchase::class,
        PurchaseItem::class,
        Expense::class,
        ExpenseCategory::class,
        Party::class,
        Due::class,
        DuePayment::class,
        MeasureUnit::class,
        CashTransaction::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MiniPosDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun partyDao(): PartyDao
    abstract fun measureUnitDao(): MeasureUnitDao
    abstract fun cashTransactionDao(): CashTransactionDao

    companion object {
        const val DB_NAME = "minipos.db"
    }
}
