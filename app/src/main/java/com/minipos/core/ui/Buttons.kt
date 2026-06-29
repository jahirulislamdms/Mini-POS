package com.minipos.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.CardCorner
import com.minipos.core.theme.OnYellow
import androidx.compose.foundation.shape.RoundedCornerShape

/** Filled blue primary action (white text). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(CardCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandYellow,
            contentColor = OnYellow,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = text)
    }
}

/** Outlined secondary action (blue outline/text). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(CardCorner),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnYellow),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = text)
    }
}
