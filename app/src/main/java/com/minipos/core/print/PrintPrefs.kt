package com.minipos.core.print

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.printDataStore: DataStore<Preferences> by preferencesDataStore(name = "minipos_print_prefs")

/** Individually toggleable receipt/invoice items (Phase 30). */
enum class ReceiptField {
    LOGO, SHOP_NAME, ADDRESS, PHONE, EMAIL, WEBSITE,
    INVOICE_NO, DATE_TIME, CUSTOMER, PAYMENT_METHOD,
    ITEMS, QUANTITY_PRICE, DISCOUNT, SUBTOTAL, GRAND_TOTAL, PAID, DUE,
    THANK_YOU, FOOTER,
}

/** Individually toggleable printed-report items (Phase 30). */
enum class ReportField { LOGO, SHOP_INFO, TITLE, GENERATED_AT, FOOTER, NOTES }

/** Receipt header/text alignment. */
enum class PrintAlign { CENTER, LEFT }

/**
 * All printing customization (Phases 29/30), stored once and applied to every printable
 * document. Disabled-field sets make new toggles default to enabled — easily extendable.
 */
data class PrintSettings(
    // Receipt paper & appearance
    val receiptWidthMm: Int = 58,
    val receiptMarginMm: Int = 3,
    val receiptFontPt: Int = 8,
    val receiptAlign: PrintAlign = PrintAlign.CENTER,
    // Receipt texts & extra contact info (Shop has no email/website fields — set here)
    val thankYouText: String = "Thank you!",
    val receiptFooter: String = "",
    val shopEmail: String = "",
    val shopWebsite: String = "",
    val receiptDisabled: Set<String> = emptySet(),
    // Reports
    val pdfPaper: String = PAPER_A4,
    val reportFontPt: Int = 10,
    val reportMarginMm: Int = 13,
    val reportFooter: String = "",
    val reportNotes: String = "",
    val reportDisabled: Set<String> = emptySet(),
) {
    fun receiptShows(f: ReceiptField): Boolean = f.name !in receiptDisabled
    fun reportShows(f: ReportField): Boolean = f.name !in reportDisabled

    companion object {
        const val PAPER_A4 = "A4"
        const val PAPER_LETTER = "LETTER"
    }
}

/**
 * Printing settings store (Phase 29/30), app-wide. Printer selection itself happens in
 * Android's system print dialog (Bluetooth / USB / Wi-Fi via installed print services), which
 * also lets any supported paper size be chosen and remembers the last used printer.
 */
class PrintPrefs(private val context: Context) {

    private val keyWidth = intPreferencesKey("receipt_width_mm")
    private val keyMargin = intPreferencesKey("receipt_margin_mm")
    private val keyFont = intPreferencesKey("receipt_font_pt")
    private val keyAlign = stringPreferencesKey("receipt_align")
    private val keyThankYou = stringPreferencesKey("receipt_thank_you")
    private val keyFooter = stringPreferencesKey("receipt_footer")
    private val keyEmail = stringPreferencesKey("shop_email")
    private val keyWebsite = stringPreferencesKey("shop_website")
    private val keyReceiptDisabled = stringPreferencesKey("receipt_disabled")
    private val keyPdfPaper = stringPreferencesKey("pdf_paper")
    private val keyReportFont = intPreferencesKey("report_font_pt")
    private val keyReportMargin = intPreferencesKey("report_margin_mm")
    private val keyReportFooter = stringPreferencesKey("report_footer")
    private val keyReportNotes = stringPreferencesKey("report_notes")
    private val keyReportDisabled = stringPreferencesKey("report_disabled")

    val settings: Flow<PrintSettings> = context.printDataStore.data.map { p ->
        PrintSettings(
            receiptWidthMm = p[keyWidth] ?: 58,
            receiptMarginMm = p[keyMargin] ?: 3,
            receiptFontPt = p[keyFont] ?: 8,
            receiptAlign = runCatching { PrintAlign.valueOf(p[keyAlign] ?: "CENTER") }
                .getOrDefault(PrintAlign.CENTER),
            thankYouText = p[keyThankYou] ?: "Thank you!",
            receiptFooter = p[keyFooter] ?: "",
            shopEmail = p[keyEmail] ?: "",
            shopWebsite = p[keyWebsite] ?: "",
            receiptDisabled = decode(p[keyReceiptDisabled]),
            pdfPaper = p[keyPdfPaper] ?: PrintSettings.PAPER_A4,
            reportFontPt = p[keyReportFont] ?: 10,
            reportMarginMm = p[keyReportMargin] ?: 13,
            reportFooter = p[keyReportFooter] ?: "",
            reportNotes = p[keyReportNotes] ?: "",
            reportDisabled = decode(p[keyReportDisabled]),
        )
    }

    suspend fun snapshot(): PrintSettings = settings.first()

    suspend fun save(s: PrintSettings) {
        context.printDataStore.edit { p ->
            p[keyWidth] = s.receiptWidthMm
            p[keyMargin] = s.receiptMarginMm
            p[keyFont] = s.receiptFontPt
            p[keyAlign] = s.receiptAlign.name
            p[keyThankYou] = s.thankYouText
            p[keyFooter] = s.receiptFooter
            p[keyEmail] = s.shopEmail
            p[keyWebsite] = s.shopWebsite
            p[keyReceiptDisabled] = s.receiptDisabled.joinToString(",")
            p[keyPdfPaper] = s.pdfPaper
            p[keyReportFont] = s.reportFontPt
            p[keyReportMargin] = s.reportMarginMm
            p[keyReportFooter] = s.reportFooter
            p[keyReportNotes] = s.reportNotes
            p[keyReportDisabled] = s.reportDisabled.joinToString(",")
        }
    }

    private fun decode(raw: String?): Set<String> =
        raw?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
}
