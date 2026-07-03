package com.minipos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.minipos.core.nav.AppRoot
import com.minipos.core.theme.MiniPosTheme
import com.minipos.feature.license.LicenseGate
import com.minipos.feature.splash.SplashScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Leave the yellow splash window theme for the normal app theme (the splash flash already showed).
        setTheme(R.style.Theme_MINIPOS)
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        setContent {
            MiniPosTheme {
                // Phase 14: a 2-second branded splash, then the normal flow (license gate → app).
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(SPLASH_MILLIS)
                    showSplash = false
                }
                Crossfade(targetState = showSplash, label = "splash") { splash ->
                    if (splash) {
                        SplashScreen()
                    } else {
                        // Licensing gate (Phase 2): the whole app sits behind a valid, device-locked license.
                        LicenseGate {
                            AppRoot()
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    private companion object {
        const val SPLASH_MILLIS = 1500L
    }
}
