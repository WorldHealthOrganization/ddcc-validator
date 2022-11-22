package org.who.ddccverifier.trust.didweb

import org.who.ddccverifier.trust.TrustRegistry
import org.who.ddccverifier.trust.TrustRegistryProvider

open class DDCCTrustRegistryProvider : TrustRegistryProvider() {
    override fun create(): TrustRegistry {
        return DDCCTrustRegistry()
    }
}