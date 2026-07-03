package com.minipos.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.minipos.data.db.MiniPosDatabase
import com.minipos.data.entity.CashTransaction
import com.minipos.data.entity.Category
import com.minipos.data.entity.Due
import com.minipos.data.entity.DuePayment
import com.minipos.data.entity.Expense
import com.minipos.data.entity.ExpenseCategory
import com.minipos.data.entity.MeasureUnit
import com.minipos.data.entity.MovementType
import com.minipos.data.entity.Party
import com.minipos.data.entity.Product
import com.minipos.data.entity.Purchase
import com.minipos.data.entity.PurchaseItem
import com.minipos.data.entity.Sale
import com.minipos.data.entity.SaleItem
import com.minipos.data.entity.Shop
import com.minipos.data.entity.ShopSettings
import com.minipos.data.entity.StockMovement
import com.minipos.data.prefs.CurrentShopManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Complete snapshot of one shop (CONVENTIONS §11). */
@Serializable
data class BackupData(
    val shop: Shop,
    val settings: ShopSettings?,
    val categories: List<Category>,
    val units: List<MeasureUnit>,
    val expenseCategories: List<ExpenseCategory>,
    val products: List<Product>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>,
    val purchases: List<Purchase>,
    val purchaseItems: List<PurchaseItem>,
    val expenses: List<Expense>,
    val parties: List<Party>,
    val dues: List<Due>,
    val duePayments: List<DuePayment>,
    val stockMovements: List<StockMovement>,
    val cashTransactions: List<CashTransaction> = emptyList(), // added in schema v3; default keeps v2 backups readable
)

@Serializable
data class BackupManifest(
    val appVersionName: String,
    val schemaVersion: Int,
    val shopName: String,
    val createdAt: Long,
    val rowCounts: Map<String, Int>,
)

sealed interface ImportResult {
    data class Success(val shopId: Long, val rows: Int) : ImportResult
    data class Failure(val message: String) : ImportResult
}

/**
 * Per-shop ZIP backup: data.json + images/ + manifest.json. Restore inserts under a NEW shopId,
 * remapping every foreign key and rewriting image paths (CONVENTIONS §11).
 */
class BackupManager(
    private val db: MiniPosDatabase,
    private val appContext: Context,
    private val currentShopManager: CurrentShopManager,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    // ---- Export ----

    /** Writes the shop's backup zip to [uri]. Returns the total row count exported. */
    suspend fun export(shopId: Long, uri: Uri): Int = withContext(Dispatchers.IO) {
        val data = collect(shopId) ?: throw IOException("Shop not found")
        val manifest = BackupManifest(
            appVersionName = appVersionName(),
            schemaVersion = CURRENT_SCHEMA_VERSION,
            shopName = data.shop.name,
            createdAt = System.currentTimeMillis(),
            rowCounts = rowCounts(data),
        )

        val out = appContext.contentResolver.openOutputStream(uri)
            ?: throw IOException("Cannot open output file")
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.writeText("manifest.json", json.encodeToString(BackupManifest.serializer(), manifest))
            zip.writeText("data.json", json.encodeToString(BackupData.serializer(), data))

            val imagePaths = buildList {
                data.shop.logoPath?.let { add(it) }
                data.products.forEach { it.photoPath?.let { p -> add(p) } }
            }
            imagePaths.forEach { rel ->
                val file = File(appContext.filesDir, rel)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry("images/$rel"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        rowCounts(data).values.sum()
    }

    private suspend fun collect(shopId: Long): BackupData? {
        val shop = db.shopDao().getShop(shopId) ?: return null
        return BackupData(
            shop = shop,
            settings = db.shopDao().getSettings(shopId),
            categories = db.categoryDao().getAllForShop(shopId),
            units = db.measureUnitDao().getAllForShop(shopId),
            expenseCategories = db.expenseDao().getCategoriesForShop(shopId),
            products = db.productDao().getAllForShop(shopId),
            sales = db.saleDao().getSalesForShop(shopId),
            saleItems = db.saleDao().getSaleItemsForShop(shopId),
            purchases = db.purchaseDao().getPurchasesForShop(shopId),
            purchaseItems = db.purchaseDao().getPurchaseItemsForShop(shopId),
            expenses = db.expenseDao().getAllForShop(shopId),
            parties = db.partyDao().getPartiesForShop(shopId),
            dues = db.partyDao().getDuesForShop(shopId),
            duePayments = db.partyDao().getPaymentsForShop(shopId),
            stockMovements = db.stockMovementDao().getAllForShop(shopId),
            cashTransactions = db.cashTransactionDao().getAllForShop(shopId),
        )
    }

    // ---- Import ----

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val entries = HashMap<String, ByteArray>()
        val input = appContext.contentResolver.openInputStream(uri)
            ?: return@withContext ImportResult.Failure("Cannot open backup file")
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val manifestBytes = entries["manifest.json"]
            ?: return@withContext ImportResult.Failure("Not a MINI POS backup (no manifest)")
        val manifest = runCatching {
            json.decodeFromString(BackupManifest.serializer(), manifestBytes.decodeToString())
        }.getOrElse { return@withContext ImportResult.Failure("Corrupt manifest") }

        if (manifest.schemaVersion !in MIN_SUPPORTED_SCHEMA_VERSION..CURRENT_SCHEMA_VERSION) {
            return@withContext ImportResult.Failure(
                "Incompatible backup (schema v${manifest.schemaVersion}, app supports " +
                    "v$MIN_SUPPORTED_SCHEMA_VERSION–v$CURRENT_SCHEMA_VERSION)",
            )
        }

        val dataBytes = entries["data.json"]
            ?: return@withContext ImportResult.Failure("Backup is missing data")
        val data = runCatching {
            json.decodeFromString(BackupData.serializer(), dataBytes.decodeToString())
        }.getOrElse { return@withContext ImportResult.Failure("Corrupt data") }

        val newShopId = restore(data, entries)
        currentShopManager.setCurrentShop(newShopId)
        ImportResult.Success(newShopId, rowCounts(data).values.sum())
    }

    private suspend fun restore(data: BackupData, images: Map<String, ByteArray>): Long =
        db.withTransaction {
            val shopDao = db.shopDao()
            val newShopId = shopDao.insertShop(data.shop.copy(id = 0, logoPath = null))
            data.settings?.let { shopDao.insertSettings(it.copy(id = 0, shopId = newShopId)) }

            if (data.units.isNotEmpty()) {
                db.measureUnitDao().insertAll(data.units.map { it.copy(id = 0, shopId = newShopId) })
            }

            // Cash transactions have no foreign keys other than shopId.
            if (data.cashTransactions.isNotEmpty()) {
                db.cashTransactionDao().insertAll(data.cashTransactions.map { it.copy(id = 0, shopId = newShopId) })
            }

            val expCatMap = HashMap<Long, Long>()
            data.expenseCategories.forEach { ec ->
                expCatMap[ec.id] = db.expenseDao().insertCategory(ec.copy(id = 0, shopId = newShopId))
            }

            // Categories: parents first so children can remap parentId.
            val catMap = HashMap<Long, Long>()
            data.categories.filter { it.parentId == null }.forEach { c ->
                catMap[c.id] = db.categoryDao().insert(c.copy(id = 0, shopId = newShopId, parentId = null))
            }
            data.categories.filter { it.parentId != null }.forEach { c ->
                catMap[c.id] = db.categoryDao().insert(
                    c.copy(id = 0, shopId = newShopId, parentId = c.parentId?.let { catMap[it] }),
                )
            }

            val partyMap = HashMap<Long, Long>()
            data.parties.forEach { p ->
                partyMap[p.id] = db.partyDao().insertParty(p.copy(id = 0, shopId = newShopId))
            }

            val productMap = HashMap<Long, Long>()
            data.products.forEach { prod ->
                val newPhoto = prod.photoPath?.let { restoreImage(it, images, newShopId, isLogo = false) }
                productMap[prod.id] = db.productDao().insert(
                    prod.copy(
                        id = 0,
                        shopId = newShopId,
                        categoryId = prod.categoryId?.let { catMap[it] },
                        subCategoryId = prod.subCategoryId?.let { catMap[it] },
                        photoPath = newPhoto,
                    ),
                )
            }
            // Phase 28: backups from before the barcode feature — generate a unique barcode for
            // every restored product that lacks one (products that had one keep it unchanged).
            data.products.forEach { prod ->
                if (prod.barcode.isNullOrBlank()) {
                    productMap[prod.id]?.let { newId ->
                        var code: String
                        do {
                            code = "2" +
                                ((System.currentTimeMillis() / 1000) % 1_000_000_000) +
                                (100..999).random()
                        } while (db.productDao().getByBarcode(newShopId, code) != null)
                        db.productDao().setBarcode(newId, code)
                    }
                }
            }

            val newLogo = data.shop.logoPath?.let { restoreImage(it, images, newShopId, isLogo = true) }
            if (newLogo != null) {
                shopDao.getShop(newShopId)?.let { shopDao.updateShop(it.copy(logoPath = newLogo)) }
            }

            val saleMap = HashMap<Long, Long>()
            data.sales.forEach { s ->
                saleMap[s.id] = db.saleDao().insertSale(
                    s.copy(id = 0, shopId = newShopId, partyId = s.partyId?.let { partyMap[it] }),
                )
            }
            if (data.saleItems.isNotEmpty()) {
                db.saleDao().insertItems(
                    data.saleItems.mapNotNull { si ->
                        saleMap[si.saleId]?.let { newSaleId ->
                            si.copy(
                                id = 0, shopId = newShopId, saleId = newSaleId,
                                productId = si.productId?.let { productMap[it] },
                            )
                        }
                    },
                )
            }

            val purchaseMap = HashMap<Long, Long>()
            data.purchases.forEach { p ->
                purchaseMap[p.id] = db.purchaseDao().insertPurchase(
                    p.copy(id = 0, shopId = newShopId, partyId = p.partyId?.let { partyMap[it] }),
                )
            }
            if (data.purchaseItems.isNotEmpty()) {
                db.purchaseDao().insertItems(
                    data.purchaseItems.mapNotNull { pi ->
                        purchaseMap[pi.purchaseId]?.let { newPid ->
                            pi.copy(
                                id = 0, shopId = newShopId, purchaseId = newPid,
                                productId = pi.productId?.let { productMap[it] },
                            )
                        }
                    },
                )
            }

            data.expenses.forEach { e ->
                db.expenseDao().insert(
                    e.copy(id = 0, shopId = newShopId, categoryId = e.categoryId?.let { expCatMap[it] }),
                )
            }

            val dueMap = HashMap<Long, Long>()
            data.dues.forEach { d ->
                val pid = partyMap[d.partyId] ?: return@forEach
                dueMap[d.id] = db.partyDao().insertDue(
                    d.copy(
                        id = 0, shopId = newShopId, partyId = pid,
                        refId = remapRef(d.refType, d.refId, saleMap, purchaseMap),
                    ),
                )
            }
            data.duePayments.forEach { dp ->
                val pid = partyMap[dp.partyId] ?: return@forEach
                db.partyDao().insertPayment(
                    dp.copy(id = 0, shopId = newShopId, partyId = pid, dueId = dp.dueId?.let { dueMap[it] }),
                )
            }

            data.stockMovements.forEach { m ->
                val pid = productMap[m.productId] ?: return@forEach
                db.stockMovementDao().insert(
                    m.copy(
                        id = 0, shopId = newShopId, productId = pid,
                        refId = remapRefByMovement(m.type, m.refId, saleMap, purchaseMap),
                    ),
                )
            }

            newShopId
        }

    private fun restoreImage(rel: String, images: Map<String, ByteArray>, newShopId: Long, isLogo: Boolean): String? {
        val bytes = images["images/$rel"] ?: return null
        val newRel = if (isLogo) {
            "shop_$newShopId/logo.jpg"
        } else {
            "shop_$newShopId/products/${rel.substringAfterLast('/')}"
        }
        val dest = File(appContext.filesDir, newRel)
        dest.parentFile?.mkdirs()
        dest.writeBytes(bytes)
        return newRel
    }

    private fun remapRef(refType: String?, refId: Long?, sales: Map<Long, Long>, purchases: Map<Long, Long>): Long? =
        when (refType) {
            "SALE" -> refId?.let { sales[it] }
            "PURCHASE" -> refId?.let { purchases[it] }
            else -> null
        }

    private fun remapRefByMovement(
        type: MovementType,
        refId: Long?,
        sales: Map<Long, Long>,
        purchases: Map<Long, Long>,
    ): Long? = when (type) {
        MovementType.SALE -> refId?.let { sales[it] }
        MovementType.PURCHASE -> refId?.let { purchases[it] }
        else -> null
    }

    private fun rowCounts(d: BackupData): Map<String, Int> = mapOf(
        "shop" to 1,
        "categories" to d.categories.size,
        "units" to d.units.size,
        "expenseCategories" to d.expenseCategories.size,
        "products" to d.products.size,
        "sales" to d.sales.size,
        "saleItems" to d.saleItems.size,
        "purchases" to d.purchases.size,
        "purchaseItems" to d.purchaseItems.size,
        "expenses" to d.expenses.size,
        "parties" to d.parties.size,
        "dues" to d.dues.size,
        "duePayments" to d.duePayments.size,
        "stockMovements" to d.stockMovements.size,
        "cashTransactions" to d.cashTransactions.size,
    )

    private fun appVersionName(): String =
        runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: "?"
        }.getOrDefault("?")

    private fun ZipOutputStream.writeText(name: String, text: String) {
        putNextEntry(ZipEntry(name))
        write(text.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 4   // v4: products carry a barcode (Phase 28)
        const val MIN_SUPPORTED_SCHEMA_VERSION = 2
    }
}
