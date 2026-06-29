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
