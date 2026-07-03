package com.minipos.core.print

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.minipos.ServiceLocator
import com.minipos.core.util.ImageStorage
import com.minipos.feature.barcodeprint.PdfPrintAdapter
import java.io.ByteArrayOutputStream
import java.io.File

/** Shared save / print / preview plumbing for every PDF in the app (Phase 29). Fully offline. */
object PdfShare {

    fun toBytes(doc: PdfDocument): ByteArray =
        ByteArrayOutputStream().use { out ->
            doc.writeTo(out)
            doc.close()
            out.toByteArray()
        }

    private fun writeToCache(context: Context, bytes: ByteArray, name: String): File {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        return File(dir, name).apply { writeBytes(bytes) }
    }

    /** Print via Android's print framework (system dialog previews & remembers the printer). */
    fun print(context: Context, bytes: ByteArray, jobName: String) {
        runCatching {
            val file = writeToCache(context, bytes, "print_job.pdf")
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(jobName, PdfPrintAdapter(file, jobName), PrintAttributes.Builder().build())
        }.onFailure {
            Toast.makeText(context, "Could not start printing", Toast.LENGTH_SHORT).show()
        }
    }

    /** Open the PDF in the device's viewer (falls back to a message when none is installed). */
    fun preview(context: Context, bytes: ByteArray, name: String) {
        runCatching {
            val file = writeToCache(context, bytes, name)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.onFailure {
            Toast.makeText(context, "No PDF viewer app found — use Print to preview", Toast.LENGTH_LONG).show()
        }
    }
}

/** One-call receipt printing for sales / purchases, using the saved print settings. */
object ReceiptPrinter {

    suspend fun printSale(context: Context, saleId: Long) {
        val settings = ServiceLocator.printPrefs.snapshot()
        val shopId = ServiceLocator.database.saleDao().getById(saleId)?.shopId ?: return
        val data = ReceiptPdf.buildSale(saleId, loadLogo(context, settings, shopId)) ?: return
        PdfShare.print(context, PdfShare.toBytes(ReceiptPdf.generate(data, settings)), "Receipt ${data.invoiceNo}")
    }

    suspend fun printPurchase(context: Context, purchaseId: Long) {
        val settings = ServiceLocator.printPrefs.snapshot()
        val shopId = ServiceLocator.database.purchaseDao().getById(purchaseId)?.shopId ?: return
        val data = ReceiptPdf.buildPurchase(purchaseId, loadLogo(context, settings, shopId)) ?: return
        PdfShare.print(context, PdfShare.toBytes(ReceiptPdf.generate(data, settings)), "Receipt ${data.invoiceNo}")
    }

    /** The shop logo bitmap when the LOGO field is enabled and a logo exists (Phase 30). */
    suspend fun loadLogo(context: Context, settings: PrintSettings, shopId: Long): Bitmap? {
        if (!settings.receiptShows(ReceiptField.LOGO)) return null
        val path = ServiceLocator.database.shopDao().getShop(shopId)?.logoPath ?: return null
        return runCatching {
            BitmapFactory.decodeFile(ImageStorage.fileFor(context, path).absolutePath)
        }.getOrNull()
    }
}
