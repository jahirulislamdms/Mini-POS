package com.minipos.core.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minipos.core.theme.CardCorner
import com.minipos.core.theme.Surface

/** White rounded card on light-grey background. Inner padding 16.dp (CONVENTIONS §6). */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = if (contentPadding) Modifier.padding(16.dp) else Modifier,
            content = content,
        )
    }
}
