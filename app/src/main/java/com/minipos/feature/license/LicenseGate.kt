package com.minipos.feature.license

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.minipos.ServiceLocator

/**
 * Licensing gate (Future Updates Phase 2). The whole app sits behind a valid, non-expired,
 * device-matched license. The license is re-verified on every start (and whenever the stored
 * key changes). Until it is valid, only the activation screen is shown; once valid, [content]
 * (the normal app) renders. A successful activation flips this automatically.
 */
@Composable
fun LicenseGate(content: @Composable () -> Unit) {
    val state by produceState<LicenseState>(LicenseState.Loading) {
        ServiceLocator.licenseManager.state.collect { value = it }
    }
    when (val s = state) {
        LicenseState.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is LicenseState.Active -> content()
        is LicenseState.Locked -> LicenseActivationScreen(deviceId = s.deviceId, reason = s.reason)
    }
}
