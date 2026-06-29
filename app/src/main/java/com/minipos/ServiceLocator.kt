package com.minipos

import android.content.Context
import androidx.room.Room
import com.minipos.data.backup.BackupManager
import com.minipos.data.db.MIGRATION_1_2
import com.minipos.data.db.MIGRATION_2_3
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.prefs.BackupReminderPrefs
import com.minipos.data.prefs.CurrentShopManager
import com.minipos.data.repo.CashRepository
import com.minipos.data.repo.CategoryRepository
import com.minipos.data.repo.ExpenseRepository
import com.minipos.data.repo.PartyRepository
import com.minipos.data.repo.ProductRepository
import com.minipos.data.repo.PurchaseRepository
import com.minipos.data.repo.SaleRepository
import com.minipos.data.repo.ShopRepository
import com.minipos.data.repo.UnitRepository

/**
 * Tiny manual DI (CONVENTIONS §2): the DB, repositories and prefs are built once here and
 * read by ViewModels in later phases. No Hilt/Dagger.
 */
object ServiceLocator {

    @Volatile private var initialized = false

    lateinit var database: MiniPosDatabase
        private set
    lateinit var shopRepository: ShopRepository
        private set
    lateinit var categoryRepository: CategoryRepository
        private set
    lateinit var productRepository: ProductRepository
        private set
    lateinit var saleRepository: SaleRepository
        private set
    lateinit var purchaseRepository: PurchaseRepository
        private set
    lateinit var expenseRepository: ExpenseRepository
        private set
    lateinit var partyRepository: PartyRepository
        private set
    lateinit var unitRepository: UnitRepository
        private set
    lateinit var cashRepository: CashRepository
        private set
    lateinit var currentShopManager: CurrentShopManager
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var backupReminderPrefs: BackupReminderPrefs
        private set

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            val db = Room.databaseBuilder(
                appContext,
                MiniPosDatabase::class.java,
                MiniPosDatabase.DB_NAME,
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()  // no destructive fallback (CONVENTIONS §8 — always migrate)

            database = db
            shopRepository = ShopRepository(db)
            categoryRepository = CategoryRepository(db.categoryDao())
            productRepository = ProductRepository(db)
            saleRepository = SaleRepository(db)
            purchaseRepository = PurchaseRepository(db)
            expenseRepository = ExpenseRepository(db.expenseDao())
            partyRepository = PartyRepository(db.partyDao())
            unitRepository = UnitRepository(db.measureUnitDao())
            cashRepository = CashRepository(db.cashTransactionDao())
            currentShopManager = CurrentShopManager(appContext)
            backupManager = BackupManager(db, appContext, currentShopManager)
            backupReminderPrefs = BackupReminderPrefs(appContext)
            initialized = true
        }
    }
}
