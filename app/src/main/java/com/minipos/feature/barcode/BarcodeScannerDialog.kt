package com.minipos.feature.barcode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.minipos.core.theme.IncomeGreen
import com.minipos.core.theme.TextMuted
import kotlinx.coroutines.delay

/**
 * Camera barcode scanner (Phase 28), fully offline (ZXing). Scans continuously and calls
 * [onScanned] for every decoded code (rapid repeats of the same code are deduped, so multiple
 * products can be scanned back-to-back). The caller decides whether to dismiss (single-shot)
 * or keep scanning, and can surface per-scan [feedback] ("Added…", "not found…").
 */
@Composable
fun BarcodeScannerDialog(
    title: String,
    feedback: String?,
    onScanned: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionAsked = true
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                if (hasPermission) {
                    // Dedupe holder survives recomposition; the callback runs on the UI thread.
                    val dedupe = remember { LastScan() }
                    var view by remember { mutableStateOf<DecoratedBarcodeView?>(null) }
                    // Phase 28.1: success flash + beep + vibration on every accepted scan.
                    var flash by remember { mutableStateOf<Pair<String, Long>?>(null) }
                    val tone = remember {
                        runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70) }.getOrNull()
                    }
                    AndroidView(
                        factory = { ctx ->
                            DecoratedBarcodeView(ctx).apply {
                                setStatusText("")
                                decodeContinuous(object : BarcodeCallback {
                                    override fun barcodeResult(result: BarcodeResult) {
                                        val text = result.text ?: return
                                        val now = System.currentTimeMillis()
                                        // Sliding window: while the camera keeps seeing the same
                                        // code, refresh the window instead of re-firing — it can
                                        // only scan again after ~2s out of view.
                                        if (text == dedupe.code && now - dedupe.at < 2_000) {
                                            dedupe.at = now
                                            return
                                        }
                                        dedupe.code = text
                                        dedupe.at = now
                                        signalScanSuccess(ctx, tone)
                                        flash = text to now
                                        onScanned(text)
                                    }

                                    override fun possibleResultPoints(resultPoints: List<ResultPoint>) = Unit
                                })
                                view = this
                            }
                        },
                        update = { it.resume() },
                        modifier = Modifier.fillMaxWidth().height(340.dp),
                    )
                    DisposableEffect(Unit) {
                        onDispose {
                            view?.pause()
                            tone?.release()
                        }
                    }
                    LaunchedEffect(flash) {
                        if (flash != null) {
                            delay(1_600)
                            flash = null
                        }
                    }
                    Column(Modifier.padding(16.dp)) {
                        flash?.let { (code, _) ->
                            Text(
                                "✓ Scanned: $code",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen,
                            )
                        }
                        Text(
                            feedback ?: "Point the camera at a barcode.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (feedback != null) MaterialTheme.colorScheme.onSurface else TextMuted,
                        )
                    }
                } else {
                    Text(
                        "Camera permission is required to scan barcodes.",
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp),
                    )
                    if (permissionAsked) {
                        TextButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                        ) { Text("Grant permission") }
                    }
                }
            }
        }
    }
}

private class LastScan {
    var code: String? = null
    var at: Long = 0
}

/** Short vibration + confirmation beep after a successful scan (Phase 28.1). Best-effort. */
private fun signalScanSuccess(context: Context, tone: ToneGenerator?) {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }
    runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120) }
}
