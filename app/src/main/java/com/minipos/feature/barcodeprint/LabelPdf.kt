package com.minipos.feature.barcodeprint

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.minipos.core.util.Money
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** What to show on each label (Phase 28 — every field user-toggleable). */
data class LabelOptions(
    val showBarcodeText: Boolean = true,
    val showName: Boolean = true,
    val showCategory: Boolean = false,
    val showSubcategory: Boolean = false,
    val showSellPrice: Boolean = true,
    val showBuyPrice: Boolean = false,
)

/** Label sheet geometry (millimetres) on an A4 page. */
data class LabelLayout(
    val widthMm: Float = 50f,
    val heightMm: Float = 30f,
    val marginMm: Float = 3f,
    val perRow: Int = 3,
    val perPage: Int = 24,
)

/** The data one label is rendered from. */
data class LabelData(
    val barcode: String,
    val name: String,
    val category: String?,
    val subcategory: String?,
    val sellPrice: Long,
    val buyPrice: Long,
)

/** Renders barcode labels (CODE-128 via ZXing, offline) into a print-ready A4 PDF. */
object LabelPdf {

    private const val PAGE_W = 595 // A4 portrait @72dpi
    private const val PAGE_H = 842

    private fun mmToPt(mm: Float): Float = mm * 72f / 25.4f

    fun generate(labels: List<LabelData>, opts: LabelOptions, layout: LabelLayout): PdfDocument {
        val doc = PdfDocument()
        val perRow = layout.perRow.coerceAtLeast(1)
        val perPage = layout.perPage.coerceAtLeast(1)
        val lw = mmToPt(layout.widthMm).coerceAtLeast(30f)
        val lh = mmToPt(layout.heightMm).coerceAtLeast(20f)
        val margin = mmToPt(layout.marginMm).coerceAtLeast(0f)

        var page: PdfDocument.Page? = null
        labels.forEachIndexed { index, label ->
            val posInPage = index % perPage
            if (posInPage == 0) {
                page?.let(doc::finishPage)
                val pageNumber = index / perPage + 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create())
            }
            val canvas = page!!.canvas
            val row = posInPage / perRow
            val col = posInPage % perRow
            val x = margin + col * (lw + margin)
            val y = margin + row * (lh + margin)
            drawLabel(canvas, label, opts, x, y, lw, lh)
        }
        page?.let(doc::finishPage)
        return doc
    }

    private fun drawLabel(
        canvas: Canvas,
        label: LabelData,
        opts: LabelOptions,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
    ) {
        // Thin cut border.
        val border = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.LTGRAY
        }
        canvas.drawRect(x, y, x + w, y + h, border)

        val lines = buildList {
            if (opts.showName) add(label.name)
            if (opts.showCategory && !label.category.isNullOrBlank()) add(label.category)
            if (opts.showSubcategory && !label.subcategory.isNullOrBlank()) add(label.subcategory)
            if (opts.showSellPrice) add("Price: ${Money.format(label.sellPrice)}")
            if (opts.showBuyPrice) add("Buy: ${Money.format(label.buyPrice)}")
            if (opts.showBarcodeText) add(label.barcode)
        }

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = (h / 9f).coerceIn(5f, 9f)
        }
        val lineH = text.textSize + 1.5f
        val textBlockH = lines.size * lineH
        val pad = 2f
        val barH = (h - textBlockH - 2 * pad).coerceAtLeast(h * 0.3f)

        // CODE-128 barcode, rendered oversampled for crisp bars.
        runCatching {
            val matrix = MultiFormatWriter().encode(
                label.barcode,
                BarcodeFormat.CODE_128,
                (w * 3).toInt().coerceAtLeast(60),
                (barH * 3).toInt().coerceAtLeast(30),
            )
            val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            for (bx in 0 until matrix.width) {
                for (by in 0 until matrix.height) {
                    bmp.setPixel(bx, by, if (matrix.get(bx, by)) Color.BLACK else Color.WHITE)
                }
            }
            canvas.drawBitmap(bmp, null, RectF(x + pad, y + pad, x + w - pad, y + pad + barH), null)
            bmp.recycle()
        }

        var ty = y + pad + barH + text.textSize
        lines.forEach { line ->
            canvas.drawText(ellipsize(line, text, w - 2 * pad), x + w / 2, ty, text)
            ty += lineH
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }
}

/** Feeds an already-generated PDF file to Android's print framework (wireless/USB/Bluetooth). */
class PdfPrintAdapter(private val file: File, private val jobName: String) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        runCatching {
            FileInputStream(file).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }.onFailure {
            callback.onWriteFailed(it.message)
        }
    }
}
