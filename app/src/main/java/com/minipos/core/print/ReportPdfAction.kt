package com.minipos.core.print

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
import com.minipos.ServiceLocator
import com.minipos.core.util.DateUtil
import com.minipos.core.util.ImageStorage
import kotlinx.coroutines.launch

/**
 * Shared "export as PDF" top-bar action for every report (Phase 29): tap → Preview / Save /
 * Print. [build] is called at action time with the report's current state and returns the PDF
 * title + lines; the configured PDF paper size (A4/Letter) is applied automatically.
 */
@Composable
fun ReportPdfAction(
    fileName: String,
    shopId: Long? = null,
    build: () -> Pair<String, List<PdfLine>>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<ByteArray?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val bytes = pendingSave
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
        }
        pendingSave = null
    }

    suspend fun bytes(): ByteArray {
        val (title, lines) = build()
        val s = ServiceLocator.printPrefs.snapshot()
        // Phase 30: report decoration (logo, shop info, generated date, footer, notes) from
        // Printing Settings — each item individually toggleable.
        val shop = shopId?.let { ServiceLocator.database.shopDao().getShop(it) }
        val logo = if (s.reportShows(ReportField.LOGO)) {
            shop?.logoPath?.let { path ->
                runCatching {
                    BitmapFactory.decodeFile(ImageStorage.fileFor(context, path).absolutePath)
                }.getOrNull()
            }
        } else {
            null
        }
        val decor = ReportDecor(
            logo = logo,
            shopName = if (s.reportShows(ReportField.SHOP_INFO)) shop?.name else null,
            shopInfo = if (s.reportShows(ReportField.SHOP_INFO)) {
                listOfNotNull(shop?.address, shop?.phone).joinToString(" · ").ifBlank { null }
            } else {
                null
            },
            showTitle = s.reportShows(ReportField.TITLE),
            generatedAt = if (s.reportShows(ReportField.GENERATED_AT)) {
                "Generated: ${DateUtil.formatDateTime(System.currentTimeMillis())}"
            } else {
                null
            },
            footer = if (s.reportShows(ReportField.FOOTER)) s.reportFooter.ifBlank { null } else null,
            notes = if (s.reportShows(ReportField.NOTES)) s.reportNotes.ifBlank { null } else null,
            fontPt = s.reportFontPt.toFloat(),
            marginPt = s.reportMarginMm * 72f / 25.4f,
        )
        return PdfShare.toBytes(ReportPdf.generate(title, lines, s.pdfPaper, decor))
    }

    IconButton(onClick = { showDialog = true }) {
        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export PDF")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Export PDF") },
            text = {
                Column {
                    TextButton(onClick = {
                        showDialog = false
                        scope.launch { PdfShare.preview(context, bytes(), fileName) }
                    }) { Text("Preview") }
                    TextButton(onClick = {
                        showDialog = false
                        scope.launch {
                            pendingSave = bytes()
                            saveLauncher.launch(fileName)
                        }
                    }) { Text("Save PDF") }
                    TextButton(onClick = {
                        showDialog = false
                        scope.launch { PdfShare.print(context, bytes(), fileName.removeSuffix(".pdf")) }
                    }) { Text("Print") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}
