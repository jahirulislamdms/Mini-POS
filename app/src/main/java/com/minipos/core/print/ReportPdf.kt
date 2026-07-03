package com.minipos.core.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument

/** One line of a generic report PDF (Phase 29). */
sealed interface PdfLine {
    data class Header(val text: String) : PdfLine
    data class KeyValue(val key: String, val value: String) : PdfLine
    data class Cols(val cells: List<String>, val bold: Boolean = false) : PdfLine
    data class Plain(val text: String) : PdfLine
    data object Divider : PdfLine
}

/** Report decoration from Printing Settings (Phase 30): logo, shop info, footer, appearance. */
data class ReportDecor(
    val logo: Bitmap? = null,
    val shopName: String? = null,
    val shopInfo: String? = null,
    val showTitle: Boolean = true,
    val generatedAt: String? = null,
    val footer: String? = null,
    val notes: String? = null,
    val fontPt: Float = 10f,
    val marginPt: Float = 36f,
)

/**
 * Generic full-page report PDF renderer (Phases 29/30): one implementation for every report — a
 * title plus a list of [PdfLine]s, rendered with the configured base paper, font and margins,
 * with automatic page breaks and optional logo/shop header + footer/notes. The Android print
 * dialog can then output to any paper size the printer supports (the layout scales). New base
 * paper sizes only need a new entry in [pageSize].
 */
object ReportPdf {

    private fun pageSize(paper: String): Pair<Int, Int> = when (paper) {
        PrintSettings.PAPER_LETTER -> 612 to 792
        else -> 595 to 842 // A4
    }

    fun generate(title: String, lines: List<PdfLine>, paper: String, decor: ReportDecor = ReportDecor()): PdfDocument {
        val (pageW, pageH) = pageSize(paper)
        val margin = decor.marginPt.coerceIn(9f, 90f)
        val fontPt = decor.fontPt.coerceIn(7f, 16f)
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = fontPt }
        val bold = Paint(body).apply { typeface = Typeface.DEFAULT_BOLD }
        val header = Paint(bold).apply { textSize = fontPt + 2 }
        val titlePaint = Paint(bold).apply { textSize = fontPt + 7 }
        val muted = Paint(body).apply { color = Color.DKGRAY }
        val lineH = fontPt * 1.5f

        val doc = PdfDocument()
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun newPage() {
            page?.let(doc::finishPage)
            pageNo += 1
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
            canvas = page!!.canvas
            y = margin
            if (pageNo == 1) {
                // Logo (auto-scaled; skipped without empty space when absent/disabled).
                decor.logo?.let { logo ->
                    val lw = 90f
                    val lh = (lw * logo.height / logo.width.toFloat()).coerceAtMost(60f)
                    canvas!!.drawBitmap(logo, null, RectF(margin, y, margin + lw, y + lh), null)
                    y += lh + lineH * 0.5f
                }
                decor.shopName?.takeIf { it.isNotBlank() }?.let {
                    y += header.textSize
                    canvas!!.drawText(it, margin, y, header)
                    y += lineH - header.textSize + lineH * 0.2f
                }
                decor.shopInfo?.takeIf { it.isNotBlank() }?.let {
                    y += muted.textSize
                    canvas!!.drawText(it, margin, y, muted)
                    y += lineH - muted.textSize
                }
                if (decor.showTitle) {
                    y += titlePaint.textSize + lineH * 0.3f
                    canvas!!.drawText(title, margin, y, titlePaint)
                    y += lineH * 0.5f
                }
                decor.generatedAt?.takeIf { it.isNotBlank() }?.let {
                    y += muted.textSize
                    canvas!!.drawText(it, margin, y, muted)
                    y += lineH - muted.textSize
                }
                y += lineH * 0.5f
            }
        }

        fun ensure(space: Float = lineH) {
            if (canvas == null || y + space > pageH - margin) newPage()
        }

        fun colXs(n: Int): List<Pair<Float, Boolean>> {
            val usable = pageW - 2 * margin
            if (n <= 1) return listOf(margin to false)
            val firstW = usable * 0.4f
            val restW = (usable - firstW) / (n - 1)
            return buildList {
                add(margin to false)
                for (i in 1 until n) add((margin + firstW + restW * i) to true)
            }
        }

        fun draw(line: PdfLine) {
            when (line) {
                is PdfLine.Header -> {
                    ensure(lineH * 2)
                    y += lineH * 0.6f + header.textSize
                    canvas!!.drawText(line.text, margin, y, header)
                    y += lineH - header.textSize
                }
                is PdfLine.KeyValue -> {
                    ensure()
                    y += body.textSize
                    canvas!!.drawText(line.key, margin, y, body)
                    bold.textAlign = Paint.Align.RIGHT
                    canvas!!.drawText(line.value, pageW - margin, y, bold)
                    bold.textAlign = Paint.Align.LEFT
                    y += lineH - body.textSize
                }
                is PdfLine.Cols -> {
                    ensure()
                    val paint = if (line.bold) bold else body
                    val xs = colXs(line.cells.size)
                    y += paint.textSize
                    line.cells.forEachIndexed { i, cell ->
                        val (x, rightAligned) = xs[i]
                        paint.textAlign = if (rightAligned) Paint.Align.RIGHT else Paint.Align.LEFT
                        val maxW = if (i == 0) (pageW - 2 * margin) * 0.38f else Float.MAX_VALUE
                        canvas!!.drawText(fit(cell, paint, maxW), x, y, paint)
                    }
                    paint.textAlign = Paint.Align.LEFT
                    y += lineH - paint.textSize
                }
                is PdfLine.Plain -> {
                    ensure()
                    y += muted.textSize
                    canvas!!.drawText(line.text, margin, y, muted)
                    y += lineH - muted.textSize
                }
                PdfLine.Divider -> {
                    ensure()
                    canvas!!.drawLine(margin, y + lineH * 0.3f, pageW - margin, y + lineH * 0.3f, muted)
                    y += lineH * 0.7f
                }
            }
        }

        newPage()
        lines.forEach(::draw)
        decor.notes?.takeIf { it.isNotBlank() }?.let {
            draw(PdfLine.Header("Notes"))
            draw(PdfLine.Plain(it))
        }
        decor.footer?.takeIf { it.isNotBlank() }?.let {
            draw(PdfLine.Divider)
            draw(PdfLine.Plain(it))
        }
        page?.let(doc::finishPage)
        return doc
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth == Float.MAX_VALUE || paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }
}
