package org.who.ddccverifier.services

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.security.PublicKey
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

object TrustRegistry {
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
        val validFrom: LocalDateTime,
        val validUntil: LocalDateTime,
        val displayName: String,
        val displayLogo: String,
        val pubKey: PublicKey
    )

    @RequiresApi(Build.VERSION_CODES.O)
    val registry = mapOf(
        "MTE=" to TrustedEntity(
            "MTE=",
            "WHO_DDCC",
            "Vaccination",
            Type.ISSUER,
            Status.CURRENT,
            "Test Keys for the WHO\'s DDCC",
            LocalDateTime.parse("2021-01-01T08:00:00"),
            LocalDateTime.parse("2021-12-01T08:00:00"),
            "WHO Test Keys",
            "",
            KeyLoader().ecPublicKeyFromPrivateKey(
                "6c1382765aec5358f117733d281c1c7bdc39884d04a45a1e6c67c858bc206c19".chunked(2)
                    .map { it.toInt(16).toByte() }
                    .toByteArray())
            )
    );

    fun resolve(kid: String): TrustedEntity? {
        return registry.get(kid);
    }
}