package org.who.ddccverifier.trust.pathcheck

import org.who.ddccverifier.trust.TrustRegistry
import org.who.ddccverifier.trust.TrustRegistryProvider

open class PCFTrustRegistryProvider : TrustRegistryProvider() {
    override fun create(): TrustRegistry {
        return PCFTrustRegistry()
    }
}