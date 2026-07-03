package com.minipos.core.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.core.util.Money
import com.minipos.data.entity.PaymentType

/** One item line on a receipt. */
data class ReceiptItem(val name: String, val qty: Double, val unitPrice: Long, val lineTotal: Long)

/** Everything a receipt needs — built from existing transaction data, nothing duplicated. */
data class ReceiptData(
    val shopName: String,
    val shopAddress: String?,
    val shopPhone: String?,
    val logo: Bitmap?,
    val invoiceNo: String,
    val dateTime: Long,
    val partyLabel: String?,      // "Customer" / "Supplier"
    val partyName: String?,
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val total: Long,
    val discount: Long,
    val paymentType: String,      // "Cash" / "Due"
    val paid: Long,
    val due: Long,
)

/**
 * Thermal-style receipt PDF (Phases 29/30): narrow page sized from the configured paper width
 * with dynamic height. Every element is individually toggleable in Printing Settings (logo,
 * shop info, invoice details, item details, amounts, thank-you & footer); alignment and font
 * size follow the settings too. Printed via Android's print framework (fully offline).
 */
object ReceiptPdf {

    private const val MM_TO_PT = 72f / 25.4f

    fun generate(d: ReceiptData, s: PrintSettings): PdfDocument {
        fun on(f: ReceiptField) = s.receiptShows(f)

        val pageW = (s.receiptWidthMm.coerceIn(30, 120) * MM_TO_PT)
        val margin = (s.receiptMarginMm.coerceIn(0, 15) * MM_TO_PT)
        val font = s.receiptFontPt.coerceIn(6, 14).toFloat()
        val lineH = font * 1.5f
        val usable = pageW - 2 * margin

        val showLogo = on(ReceiptField.LOGO) && d.logo != null
        val logoH = if (showLogo) {
            val ratio = d.logo!!.height.toFloat() / d.logo.width.toFloat()
            (usable * 0.45f * ratio).coerceAtMost(usable * 0.5f)
        } else {
            0f
        }

        // Count lines to size the page.
        var count = 1f
        if (on(ReceiptField.SHOP_NAME)) count += 1.6f
        if (on(ReceiptField.ADDRESS) && !d.shopAddress.isNullOrBlank()) count += 1
        if (on(ReceiptField.PHONE) && !d.shopPhone.isNullOrBlank()) count += 1
        if (on(ReceiptField.EMAIL) && s.shopEmail.isNotBlank()) count += 1
        if (on(ReceiptField.WEBSITE) && s.shopWebsite.isNotBlank()) count += 1
        count += 1 // divider
        if (on(ReceiptField.INVOICE_NO)) count += 1
        if (on(ReceiptField.DATE_TIME)) count += 1
        if (on(ReceiptField.CUSTOMER) && d.partyName != null) count += 1
        count += 1 // divider
        if (on(ReceiptField.ITEMS)) {
            count += d.items.size * (if (on(ReceiptField.QUANTITY_PRICE)) 2f else 1f)
            count += 1 // divider
        }
        if (on(ReceiptField.SUBTOTAL)) count += 1
        if (on(ReceiptField.DISCOUNT) && d.discount > 0) count += 1
        if (on(ReceiptField.GRAND_TOTAL)) count += 1
        if (on(ReceiptField.PAYMENT_METHOD)) count += 1
        if (on(ReceiptField.PAID)) count += 1
        if (on(ReceiptField.DUE) && d.due > 0) count += 1
        if (on(ReceiptField.THANK_YOU) && s.thankYouText.isNotBlank()) count += 1.5f
        if (on(ReceiptField.FOOTER) && s.receiptFooter.isNotBlank()) count += 1
        val pageH = margin * 2 + logoH + (count + 2) * lineH

        val doc = PdfDocument()
        val page = doc.startPage(
            PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), 1).create(),
        )
        val canvas = page.canvas

        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = font }
        val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
        val title = Paint(bold).apply { textSize = font + 3 }
        val center = pageW / 2
        val left = margin
        val right = pageW - margin
        var y = margin

        fun divider() {
            y += lineH * 0.3f
            val dash = Paint(body).apply { color = Color.DKGRAY; strokeWidth = 0.6f }
            canvas.drawLine(left, y, right, y, dash)
            y += lineH * 0.9f
        }

        /** Header lines follow the configured alignment (center or left). */
        fun headerLine(text: String, paint: Paint) {
            if (s.receiptAlign == PrintAlign.CENTER) {
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(fit(text, paint, usable), center, y + paint.textSize, paint)
                paint.textAlign = Paint.Align.LEFT
            } else {
                canvas.drawText(fit(text, paint, usable), left, y + paint.textSize, paint)
            }
            y += lineH
        }

        fun leftRight(l: String, r: String, paint: Paint) {
            y += paint.textSize
            val rw = paint.measureText(r)
            canvas.drawText(fit(l, paint, usable - rw - 4), left, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(r, right, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += lineH - paint.textSize
        }

        // Logo (auto-scaled; no empty space when absent or disabled)
        if (showLogo) {
            val lw = usable * 0.45f
            val lx = when (s.receiptAlign) {
                PrintAlign.CENTER -> center - lw / 2
                PrintAlign.LEFT -> left
            }
            canvas.drawBitmap(d.logo!!, null, RectF(lx, y, lx + lw, y + logoH), null)
            y += logoH + lineH * 0.3f
        }

        // Header
        if (on(ReceiptField.SHOP_NAME)) {
            headerLine(d.shopName, title)
            y += lineH * 0.2f
        }
        if (on(ReceiptField.ADDRESS)) d.shopAddress?.takeIf { it.isNotBlank() }?.let { headerLine(it, body) }
        if (on(ReceiptField.PHONE)) d.shopPhone?.takeIf { it.isNotBlank() }?.let { headerLine(it, body) }
        if (on(ReceiptField.EMAIL) && s.shopEmail.isNotBlank()) headerLine(s.shopEmail, body)
        if (on(ReceiptField.WEBSITE) && s.shopWebsite.isNotBlank()) headerLine(s.shopWebsite, body)
        divider()

        if (on(ReceiptField.INVOICE_NO)) leftRight("Invoice", d.invoiceNo, body)
        if (on(ReceiptField.DATE_TIME)) leftRight("Date", DateUtil.formatDateTime(d.dateTime), body)
        if (on(ReceiptField.CUSTOMER) && d.partyName != null) {
            leftRight(d.partyLabel ?: "Party", d.partyName, body)
        }
        divider()

        // Items
        if (on(ReceiptField.ITEMS)) {
            d.items.forEach { item ->
                if (on(ReceiptField.QUANTITY_PRICE)) {
                    y += bold.textSize
                    canvas.drawText(fit(item.name, bold, usable), left, y, bold)
                    y += lineH - bold.textSize
                    leftRight("  ${item.qty.qty()} x ${Money.format(item.unitPrice)}", Money.format(item.lineTotal), body)
                } else {
                    leftRight(item.name, Money.format(item.lineTotal), body)
                }
            }
            divider()
        }

        if (on(ReceiptField.SUBTOTAL)) leftRight("Subtotal", Money.format(d.subtotal), body)
        if (on(ReceiptField.DISCOUNT) && d.discount > 0) leftRight("Discount", Money.format(d.discount), body)
        if (on(ReceiptField.GRAND_TOTAL)) leftRight("TOTAL", Money.format(d.total), bold)
        if (on(ReceiptField.PAYMENT_METHOD)) leftRight("Payment", d.paymentType, body)
        if (on(ReceiptField.PAID)) leftRight("Paid", Money.format(d.paid), body)
        if (on(ReceiptField.DUE) && d.due > 0) leftRight("Due", Money.format(d.due), bold)

        if (on(ReceiptField.THANK_YOU) && s.thankYouText.isNotBlank()) {
            y += lineH * 0.5f
            headerLine(s.thankYouText, bold)
        }
        if (on(ReceiptField.FOOTER) && s.receiptFooter.isNotBlank()) {
            headerLine(s.receiptFooter, body)
        }

        doc.finishPage(page)
        return doc
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }

    private fun Double.qty(): String =
        if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

    /** Build receipt data for a sale from the existing records. */
    suspend fun buildSale(saleId: Long, logo: Bitmap?): ReceiptData? {
        val db = ServiceLocator.database
        val sale = db.saleDao().getById(saleId) ?: return null
        val shop = db.shopDao().getShop(sale.shopId) ?: return null
        val items = db.saleDao().getItems(saleId)
        val party = sale.partyId?.let { db.partyDao().getParty(it)?.name }
        return ReceiptData(
            shopName = shop.name,
            shopAddress = shop.address,
            shopPhone = shop.phone,
            logo = logo,
            invoiceNo = "INV-S%06d".format(sale.id),
            dateTime = sale.createdAt,
            partyLabel = "Customer",
            partyName = party,
            items = items.map { ReceiptItem(it.name, it.quantity, it.unitPrice, it.lineTotal) },
            subtotal = sale.subtotal,
            total = sale.total,
            discount = sale.discount,
            paymentType = if (sale.paymentType == PaymentType.DUE) "Due" else "Cash",
            paid = sale.paidAmount,
            due = sale.dueAmount,
        )
    }

    /** Build receipt data for a purchase from the existing records. */
    suspend fun buildPurchase(purchaseId: Long, logo: Bitmap?): ReceiptData? {
        val db = ServiceLocator.database
        val purchase = db.purchaseDao().getById(purchaseId) ?: return null
        val shop = db.shopDao().getShop(purchase.shopId) ?: return null
        val items = db.purchaseDao().getItems(purchaseId)
        val party = purchase.partyId?.let { db.partyDao().getParty(it)?.name }
        return ReceiptData(
            shopName = shop.name,
            shopAddress = shop.address,
            shopPhone = shop.phone,
            logo = logo,
            invoiceNo = "INV-B%06d".format(purchase.id),
            dateTime = purchase.createdAt,
            partyLabel = "Supplier",
            partyName = party,
            items = items.map { ReceiptItem(it.name, it.quantity, it.unitPrice, it.lineTotal) },
            subtotal = purchase.subtotal,
            total = purchase.total,
            discount = purchase.discount,
            paymentType = if (purchase.paymentType == PaymentType.DUE) "Due" else "Cash",
            paid = purchase.paidAmount,
            due = purchase.dueAmount,
        )
    }
}
