package com.minipos.feature.license

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.licenseDataStore: DataStore<Preferences> by preferencesDataStore(name = "minipos_license")

/** Why the app is locked (when not [LicenseState.Active]). */
enum class LockReason { NONE, EXPIRED, WRONG_DEVICE, INVALID }

/** Live state of the licensing gate. */
sealed interface LicenseState {
    data object Loading : LicenseState
    data class Locked(val reason: LockReason, val deviceId: String) : LicenseState
    data class Active(val expiryMillis: Long, val deviceId: String) : LicenseState
}

/** Outcome of an activation / renewal / replacement attempt. */
sealed interface ActivationResult {
    data class Success(val expiryMillis: Long) : ActivationResult
    data class Failure(val message: String) : ActivationResult
}

/**
 * Owns the per-install Device ID and the stored license key (DataStore in app-private storage,
 * separate from all POS data) and exposes the live [state]. Verification is 100% offline via
 * [LicenseVerifier]. This module shares nothing with the POS/Room logic, and updating the
 * license never touches shop or user data.
 */
class LicenseManager(private val context: Context) {

    private val keyDeviceId = stringPreferencesKey("device_id")
    private val keyLicense = stringPreferencesKey("license_key")

    @Volatile private var cachedDeviceId: String? = null

    /** Stable per-install Device ID: generated once on first run and persisted permanently. */
    suspend fun deviceId(): String {
        cachedDeviceId?.let { return it }
        val stored = context.licenseDataStore.data.map { it[keyDeviceId] }.first()
        val id = stored ?: newDeviceId().also { generated ->
            context.licenseDataStore.edit { it[keyDeviceId] = generated }
        }
        cachedDeviceId = id
        return id
    }

    /**
     * Live license state, re-evaluated whenever the stored key changes — and freshly verified
     * (including the expiry check) on every collection / app start.
     */
    val state: Flow<LicenseState> = context.licenseDataStore.data.map { prefs ->
        val id = deviceId()
        val key = prefs[keyLicense]
        if (key.isNullOrBlank()) {
            LicenseState.Locked(LockReason.NONE, id)
        } else {
            when (val r = LicenseVerifier.verify(key, id, System.currentTimeMillis())) {
                is VerifyResult.Valid -> LicenseState.Active(r.expiryMillis, id)
                is VerifyResult.Expired -> LicenseState.Locked(LockReason.EXPIRED, id)
                VerifyResult.WrongDevice -> LicenseState.Locked(LockReason.WRONG_DEVICE, id)
                is VerifyResult.Invalid -> LicenseState.Locked(LockReason.INVALID, id)
            }
        }
    }

    /** Validate [rawKey] for this device and, on success, persist it (used by Activate/Renew/Replace). */
    suspend fun activate(rawKey: String): ActivationResult {
        val id = deviceId()
        return when (val r = LicenseVerifier.verify(rawKey.trim(), id, System.currentTimeMillis())) {
            is VerifyResult.Valid -> {
                context.licenseDataStore.edit { it[keyLicense] = rawKey.trim() }
                ActivationResult.Success(r.expiryMillis)
            }
            is VerifyResult.Expired -> ActivationResult.Failure("This license has expired. Please request a renewal.")
            VerifyResult.WrongDevice -> ActivationResult.Failure("This license belongs to another device.")
            is VerifyResult.Invalid -> ActivationResult.Failure(r.message)
        }
    }

    /** A readable, unique Device ID, e.g. MPOS-A1B2-C3D4-E5F6-7890. */
    private fun newDeviceId(): String {
        val hex = UUID.randomUUID().toString().replace("-", "").uppercase().take(16)
        return "MPOS-" + hex.chunked(4).joinToString("-")
    }
}
