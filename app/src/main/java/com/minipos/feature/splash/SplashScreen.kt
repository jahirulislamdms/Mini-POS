package com.minipos.feature.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minipos.R
import com.minipos.core.theme.BrandYellow
import com.minipos.core.theme.OnYellow
import kotlin.math.PI
import kotlin.math.sin

/**
 * Branded splash (Phase 14): yellow background, centred MINI POS logo, and the app name below
 * with a subtle wave animation. Shown for ~2s on launch; purely cosmetic — no app logic here.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(BrandYellow),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "MINI POS logo",
                modifier = Modifier.size(168.dp),
            )
            Spacer(Modifier.height(4.dp))
            WaveText(text = "MINI POS", color = OnYellow)
        }
    }
}

/** App name with a gentle left-to-right wave (each letter bobs slightly out of phase). */
@Composable
private fun WaveText(text: String, color: Color) {
    val transition = rememberInfiniteTransition(label = "splashWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        text.forEachIndexed { index, ch ->
            val yOffset = (sin(phase + index * 0.45f) * 8f).dp
            Text(
                text = ch.toString(),
                color = color,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(y = yOffset),
            )
        }
    }
}
