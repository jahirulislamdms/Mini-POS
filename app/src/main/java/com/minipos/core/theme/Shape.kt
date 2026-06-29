package com.minipos.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Cards/buttons 12.dp; top bars square (handled per-component). CONVENTIONS §6.
val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
)

// Shared corner radius for cards & buttons.
val CardCorner = 12.dp
