package org.who.ddccverifier.trust

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

    data class TrustedEntity(
        val displayName: Map<String, String>,
        val displayLogo: String?,
        val status: Status,
        val validFrom: Date?,
        val validUntil: Date?,
        val publicKey: PublicKey
    )

    fun init()
    fun init(vararg customUrls: String)
    fun resolve(framework: Framework, kid: String): TrustedEntity?
}