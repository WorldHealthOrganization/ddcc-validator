package org.who.ddccverifier.services.trust

import org.who.ddccverifier.trust.TrustRegistry
import org.who.ddccverifier.trust.pathcheck.PCFTrustRegistry
/**
 * Resolve Keys for Verifiers
 */
object TrustRegistrySingleton {
    private val registry: TrustRegistry by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val reg = PCFTrustRegistry();
        reg.init();
        reg.addTestKeys()

        return@lazy reg
    }

    fun get(): TrustRegistry {
        return registry;
    }

    fun init() {
        // dummy key
        get().resolve(TrustRegistry.Framework.DCC, "MTE=")
    }

}