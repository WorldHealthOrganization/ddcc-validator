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
        val validFromDT: Date?,
        val validUntilDT: Date?,
        val didDocument: PublicKey
    )

    fun init()
    fun resolve(framework: Framework, kid: String): TrustedEntity?
}