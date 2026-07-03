package com.minipos.feature.license

import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Result of verifying a license key — fully offline, no network. */
sealed interface VerifyResult {
    data class Valid(val expiryMillis: Long) : VerifyResult
    data class Expired(val expiryMillis: Long) : VerifyResult
    data object WrongDevice : VerifyResult
    data class Invalid(val message: String) : VerifyResult
}

/**
 * Offline license verification. Mirrors the Python generator exactly:
 *
 *   key      = "<payloadB64>.<signatureB64>"
 *   payload  = "<deviceId>|<expiryMillis>"
 *   signature = SHA256withRSA (PKCS#1 v1.5) over the payloadB64 ASCII bytes
 *
 * Verified with the embedded RSA public key ([LicenseKeys.PUBLIC_KEY_B64]). Because only the
 * public key ships, keys can be checked but never forged. No internet, ever.
 */
object LicenseVerifier {

    private fun publicKey() =
        KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(Base64.decode(LicenseKeys.PUBLIC_KEY_B64, Base64.NO_WRAP)),
        )

    fun verify(licenseKey: String, deviceId: String, nowMillis: Long): VerifyResult {
        val parts = licenseKey.trim().split(".")
        if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return VerifyResult.Invalid("Invalid license key format.")
        }
        val payloadB64 = parts[0]
        val signature = try {
            Base64.decode(parts[1], Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return VerifyResult.Invalid("Invalid license key.")
        }

        val genuine = try {
            Signature.getInstance("SHA256withRSA").run {
                initVerify(publicKey())
                update(payloadB64.toByteArray(Charsets.US_ASCII))
                verify(signature)
            }
        } catch (_: Exception) {
            false
        }
        if (!genuine) return VerifyResult.Invalid("This license key is not valid.")

        val payload = try {
            String(Base64.decode(payloadB64, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return VerifyResult.Invalid("Invalid license key.")
        }
        val seg = payload.split("|")
        if (seg.size != 2) return VerifyResult.Invalid("Invalid license key.")
        val licenseDevice = seg[0]
        val expiry = seg[1].toLongOrNull() ?: return VerifyResult.Invalid("Invalid license key.")

        return when {
            licenseDevice != deviceId -> VerifyResult.WrongDevice
            nowMillis > expiry -> VerifyResult.Expired(expiry)
            else -> VerifyResult.Valid(expiry)
        }
    }
}
