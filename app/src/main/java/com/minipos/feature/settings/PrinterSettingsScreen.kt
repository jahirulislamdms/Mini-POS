package com.minipos.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.minipos.ServiceLocator
import com.minipos.core.print.PdfShare
import com.minipos.core.print.PrintAlign
import com.minipos.core.print.PrintSettings
import com.minipos.core.print.ReceiptData
import com.minipos.core.print.ReceiptField
import com.minipos.core.print.ReceiptItem
import com.minipos.core.print.ReceiptPdf
import com.minipos.core.print.ReportField
import com.minipos.core.theme.AppBackground
import com.minipos.core.theme.TextMuted
import com.minipos.core.ui.AppCard
import com.minipos.core.ui.AppTextField
import com.minipos.core.ui.AppTopBar
import com.minipos.core.ui.PrimaryButton
import com.minipos.core.ui.SecondaryButton
import com.minipos.core.ui.SectionHeader
import kotlinx.coroutines.launch

/**
 * Settings → Printing Settings (Phases 29/30): the single place for ALL print customization —
 * receipt paper/appearance, per-item receipt content, report appearance and per-item report
 * decoration — stored once and applied to every printable document (receipts, reports, labels).
 * Printer connection & paper size selection happen in Android's system print dialog (Bluetooth /
 * USB / Wi-Fi via installed print services; any paper the printer supports).
 */
@Composable
fun PrinterSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var loaded by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(PrintSettings()) }
    var widthMm by remember { mutableStateOf("58") }
    var marginMm by remember { mutableStateOf("3") }
    var fontPt by remember { mutableStateOf("8") }
    var reportFontPt by remember { mutableStateOf("10") }
    var reportMarginMm by remember { mutableStateOf("13") }

    LaunchedEffect(Unit) {
        val s = ServiceLocator.printPrefs.snapshot()
        settings = s
        widthMm = s.receiptWidthMm.toString()
        marginMm = s.receiptMarginMm.toString()
        fontPt = s.receiptFontPt.toString()
        reportFontPt = s.reportFontPt.toString()
        reportMarginMm = s.reportMarginMm.toString()
        loaded = true
    }

    fun currentSettings() = settings.copy(
        receiptWidthMm = widthMm.toIntOrNull() ?: 58,
        receiptMarginMm = marginMm.toIntOrNull() ?: 3,
        receiptFontPt = fontPt.toIntOrNull() ?: 8,
        reportFontPt = reportFontPt.toIntOrNull() ?: 10,
        reportMarginMm = reportMarginMm.toIntOrNull() ?: 13,
    )

    fun toggleReceipt(f: ReceiptField) {
        settings = settings.copy(
            receiptDisabled = if (f.name in settings.receiptDisabled) {
                settings.receiptDisabled - f.name
            } else {
                settings.receiptDisabled + f.name
            },
        )
    }

    fun toggleReport(f: ReportField) {
        settings = settings.copy(
            reportDisabled = if (f.name in settings.reportDisabled) {
                settings.reportDisabled - f.name
            } else {
                settings.reportDisabled + f.name
            },
        )
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = "Printing Settings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader("Printer connection")
            AppCard {
                Text(
                    "Printing uses Android's print service — Bluetooth, USB and Wi-Fi/network " +
                        "printers are supported through your installed printer app. The system " +
                        "print dialog remembers your printer and lets you pick ANY paper size the " +
                        "printer supports; documents scale to fit automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            SectionHeader("Receipt paper & appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    AppTextField(widthMm, { widthMm = it }, "Width (mm)", keyboardType = KeyboardType.Number)
                }
                Column(Modifier.weight(1f)) {
                    AppTextField(marginMm, { marginMm = it }, "Margin (mm)", keyboardType = KeyboardType.Number)
                }
                Column(Modifier.weight(1f)) {
                    AppTextField(fontPt, { fontPt = it }, "Font size", keyboardType = KeyboardType.Number)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    settings.receiptAlign == PrintAlign.CENTER,
                    { settings = settings.copy(receiptAlign = PrintAlign.CENTER) },
                    label = { Text("Center") },
                )
                FilterChip(
                    settings.receiptAlign == PrintAlign.LEFT,
                    { settings = settings.copy(receiptAlign = PrintAlign.LEFT) },
                    label = { Text("Left") },
                )
            }

            SectionHeader("Receipt content")
            AppCard {
                ReceiptField.entries.forEach { f ->
                    ToggleRow(receiptLabel(f), settings.receiptShows(f)) { toggleReceipt(f) }
                }
            }
            AppTextField(
                settings.thankYouText, { settings = settings.copy(thankYouText = it) },
                "Thank-you message",
            )
            AppTextField(
                settings.receiptFooter, { settings = settings.copy(receiptFooter = it) },
                "Custom footer message",
            )
            AppTextField(settings.shopEmail, { settings = settings.copy(shopEmail = it) }, "Email (printed)")
            AppTextField(settings.shopWebsite, { settings = settings.copy(shopWebsite = it) }, "Website (printed)")

            SectionHeader("Report appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    settings.pdfPaper == PrintSettings.PAPER_A4,
                    { settings = settings.copy(pdfPaper = PrintSettings.PAPER_A4) },
                    label = { Text("A4") },
                )
                FilterChip(
                    settings.pdfPaper == PrintSettings.PAPER_LETTER,
                    { settings = settings.copy(pdfPaper = PrintSettings.PAPER_LETTER) },
                    label = { Text("Letter") },
                )
            }
            Text(
                "Base layout size — the print dialog can still output to any printer paper.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    AppTextField(reportFontPt, { reportFontPt = it }, "Font size", keyboardType = KeyboardType.Number)
                }
                Column(Modifier.weight(1f)) {
                    AppTextField(reportMarginMm, { reportMarginMm = it }, "Margins (mm)", keyboardType = KeyboardType.Number)
                }
            }

            SectionHeader("Report content")
            AppCard {
                ReportField.entries.forEach { f ->
                    ToggleRow(reportLabel(f), settings.reportShows(f)) { toggleReport(f) }
                }
            }
            AppTextField(
                settings.reportFooter, { settings = settings.copy(reportFooter = it) },
                "Report footer",
            )
            AppTextField(
                settings.reportNotes, { settings = settings.copy(reportNotes = it) },
                "Custom notes (printed on reports)",
            )

            PrimaryButton(
                text = "Save settings",
                enabled = loaded,
                onClick = {
                    scope.launch {
                        val s = currentSettings()
                        settings = s
                        ServiceLocator.printPrefs.save(s)
                        snackbar.showSnackbar("Printing settings saved")
                    }
                },
            )
            SecondaryButton(
                text = "Print test receipt",
                enabled = loaded,
                onClick = {
                    scope.launch {
                        val s = currentSettings()
                        settings = s
                        ServiceLocator.printPrefs.save(s)
                        val sample = ReceiptData(
                            shopName = "MINI POS",
                            shopAddress = "Test print",
                            shopPhone = null,
                            logo = null,
                            invoiceNo = "TEST-000001",
                            dateTime = System.currentTimeMillis(),
                            partyLabel = null,
                            partyName = null,
                            items = listOf(ReceiptItem("Sample product", 2.0, 5_000, 10_000)),
                            subtotal = 10_000,
                            total = 10_000,
                            discount = 0,
                            paymentType = "Cash",
                            paid = 10_000,
                            due = 0,
                        )
                        PdfShare.print(context, PdfShare.toBytes(ReceiptPdf.generate(sample, s)), "Test receipt")
                    }
                },
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

private fun receiptLabel(f: ReceiptField): String = when (f) {
    ReceiptField.LOGO -> "Shop logo"
    ReceiptField.SHOP_NAME -> "Shop name"
    ReceiptField.ADDRESS -> "Shop address"
    ReceiptField.PHONE -> "Phone number"
    ReceiptField.EMAIL -> "Email"
    ReceiptField.WEBSITE -> "Website"
    ReceiptField.INVOICE_NO -> "Invoice number"
    ReceiptField.DATE_TIME -> "Invoice date & time"
    ReceiptField.CUSTOMER -> "Customer / supplier info"
    ReceiptField.PAYMENT_METHOD -> "Payment method"
    ReceiptField.ITEMS -> "Product details"
    ReceiptField.QUANTITY_PRICE -> "Quantity & unit price"
    ReceiptField.DISCOUNT -> "Discount"
    ReceiptField.SUBTOTAL -> "Subtotal"
    ReceiptField.GRAND_TOTAL -> "Grand total"
    ReceiptField.PAID -> "Paid amount"
    ReceiptField.DUE -> "Due amount"
    ReceiptField.THANK_YOU -> "Thank-you message"
    ReceiptField.FOOTER -> "Custom footer"
}

private fun reportLabel(f: ReportField): String = when (f) {
    ReportField.LOGO -> "Shop logo"
    ReportField.SHOP_INFO -> "Shop information"
    ReportField.TITLE -> "Report title"
    ReportField.GENERATED_AT -> "Generated date & time"
    ReportField.FOOTER -> "Footer"
    ReportField.NOTES -> "Custom notes"
}
