package com.minipos.feature.license

/**
 * Embedded licensing **public** key (RSA-2048, X.509/SubjectPublicKeyInfo DER, base64).
 *
 * This is safe to ship: it can only *verify* licenses, never create them. The matching
 * **private** key lives only in the owner-only `license_generator/` project (git-ignored) and
 * never ships. If the keypair is ever regenerated, replace this value with the output of
 * `python main.py pubkey` and rebuild.
 */
object LicenseKeys {
    const val PUBLIC_KEY_B64: String =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA453kfYsXMjEbYa94wXHvhEog3JLIGSzjMxxHHsD95SWohypCVTn0Wx7P/GRtE5AwKf76JylNAzsc0OJ7LROQ/NGNFcoMs733yyTfHRJlhLqp2dxrruMK7WrhtoBylYF2PQNp5wHGzvLOZMOjCaB0EWlh8Fu2iooKAEZM13FOvAM+/N8npCqz9o43lNHM73ey6vCOasQid9+TO7hP7mtD5vpSgV9xKU9lyWNJiKl/4GljPPG+YwZmu/6TQc7ssh+a7v1iogWGlFGtwx7EdP5JOsb3gfneguadWosKkKCJUt2alBREn2W2QRshENaskCi1URqrMmGkH9BtNVFUdo4M2QIDAQAB"
}
