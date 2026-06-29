package com.minipos.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.minipos.core.theme.AppBackground

/** Temporary scaffold-backed screen used for P1 nav skeleton; replaced per feature in later phases. */
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
) {
    Scaffold(
        containerColor = AppBackground,
        topBar = { AppTopBar(title = title) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                EmptyState(
                    message = message,
                    icon = Icons.Outlined.Construction,
                )
            }
        }
    }
}
