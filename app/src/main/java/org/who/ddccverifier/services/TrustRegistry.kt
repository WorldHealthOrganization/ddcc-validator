package org.who.ddccverifier.services

import java.security.PublicKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object TrustRegistry {
    // Using old java.time to keep compatibility down to Android SDK 22.
    private var df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    enum class Status {
        CURRENT, EXPIRED, TERMINATED, REVOKED
    }
    enum class Type {
        ISSUER, VERIFIER, TRUST_REGISTRY
    }
    data class TrustedEntity(
        val identifier: String,
        val governanceFramework: String,
        val credentialType: String,
        val entityType: Type,
        val status: Status,
        val statusDetail: String,
        val validFrom: Date?,
        val validUntil: Date?,
        val displayName: String,
        val displayLogo: String,
        val pubKey: PublicKey
    )

    private val registry = mapOf(
        "MTE=" to TrustedEntity(
            "MTE=",
            "WHO_DDCC",
            "Vaccination",
            Type.ISSUER,
            Status.CURRENT,
            "Test Keys for the WHO\'s DDCC",
            df.parse("2021-01-01T08:00:00.000Z"),
            df.parse("2021-12-01T08:00:00.000Z"),
            "WHO Test Keys",
            "",
            KeyLoader().ecPublicKeyFromCoordinate(
                "143329cce7868e416927599cf65a34f3ce2ffda55a7eca69ed8919a394d42f0f".chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray(),
                "60f7f1a780d8a783bfb7a2dd6b2796e8128dbbcef9d3d168db9529971a36e7b9".chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray())
            )
    )

    fun resolve(kid: String): TrustedEntity? {
        return registry[kid]
    }
}