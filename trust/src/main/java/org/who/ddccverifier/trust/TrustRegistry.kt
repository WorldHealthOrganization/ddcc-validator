package org.who.ddccverifier.trust

import java.net.URI
import java.security.PublicKey
import java.util.*

/**
 * Trust Registry Interface
 */
interface TrustRegistry {
    enum class Status {
        CURRENT, EXPIRED, TERMINATED, REVOKED
    }
    enum class Framework {
        CRED, DCC, ICAO, SHC, DIVOC
    }
    enum class Scope {
        PRODUCTION, ACCEPTANCE_TEST
    }

    data class RegistryEntity(
        val scope: Scope,
        val resolvableURI: URI,
        val publicKey: PublicKey?
    )

    data class TrustedEntity(
        val displayName: Map<String, String>,
        val displayLogo: String?,
        val status: Status,
        val scope: Scope,
        val validFrom: Date?,
        val validUntil: Date?,
        val publicKey: PublicKey
    )

    fun init()
    fun init(vararg customRegistries: RegistryEntity)
    fun resolve(framework: Framework, kid: String): TrustedEntity?
}