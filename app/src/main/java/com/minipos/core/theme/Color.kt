package com.minipos.core.theme

import androidx.compose.ui.graphics.Color

// MINI POS brand tokens (CONVENTIONS §5). Use ONLY these — no ad-hoc colors per screen.
val BrandYellow = Color(0xFFFFC107)   // top app bars
val OnYellow = Color(0xFF1F2937)      // text/icons on yellow
val PrimaryBlue = Color(0xFF1565C0)   // retained token (no longer the primary; app is yellow-primary)
val OnBlue = Color(0xFFFFFFFF)        // white — used as on-error (content on red)
val IncomeGreen = Color(0xFF16A34A)   // money in / positive
val ExpenseRed = Color(0xFFE53935)    // money out / due / negative
val AppBackground = Color(0xFFF4F5F7) // screen background (light grey)
val Surface = Color(0xFFFFFFFF)       // cards
val OnSurface = Color(0xFF1F2937)     // primary text
val TextMuted = Color(0xFF6B7280)     // secondary text / hints
val Divider = Color(0xFFE5E7EB)
val DueReceiveBg = Color(0xFFFCEBEA)  // light red tile (you'll receive)
val DueGiveBg = Color(0xFFE9F7EF)     // light green tile (you'll give)
