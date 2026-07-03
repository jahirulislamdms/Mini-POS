package com.minipos.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: add the `units` table (custom measurement units, BUILD_PLAN §3 / P4.1).
 * CONVENTIONS §8: always migrate, never destructive-fallback.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `units` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`shopId` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_units_shopId` ON `units` (`shopId`)")
    }
}

/**
 * v2 -> v3: add the `cash_transactions` table (manual Cash In / Cash Out adjustments).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_transactions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`shopId` INTEGER NOT NULL, " +
                "`amount` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`note` TEXT, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_shopId` ON `cash_transactions` (`shopId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_createdAt` ON `cash_transactions` (`createdAt`)")
    }
}

/**
 * v3 -> v4: add the `activity_undos` table (Phase 15 — audit trail of undone activities).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `activity_undos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`shopId` INTEGER NOT NULL, " +
                "`targetType` TEXT NOT NULL, " +
                "`targetRefId` INTEGER NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_undos_shopId` ON `activity_undos` (`shopId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_undos_createdAt` ON `activity_undos` (`createdAt`)")
    }
}

/**
 * v4 -> v5: add the `cash_drawer_openings` table (Phase 27 — per-day Opening Cash).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_drawer_openings` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`shopId` INTEGER NOT NULL, " +
                "`dayStart` INTEGER NOT NULL, " +
                "`amount` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_drawer_openings_shopId` ON `cash_drawer_openings` (`shopId`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_drawer_openings_shopId_dayStart` " +
                "ON `cash_drawer_openings` (`shopId`, `dayStart`)",
        )
    }
}

/**
 * v5 -> v6: add `products.barcode` (Phase 28 — barcode management). Uniqueness is enforced
 * per shop in app logic; missing barcodes are backfilled on app start.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `barcode` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
    }
}

/**
 * v6 -> v7: add `shop_settings.defaultUnit` (Phase 31 — pre-selected unit for new products).
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `shop_settings` ADD COLUMN `defaultUnit` TEXT NOT NULL DEFAULT 'pcs'")
    }
}
