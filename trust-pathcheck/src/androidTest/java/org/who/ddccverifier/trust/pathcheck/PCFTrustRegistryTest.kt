package org.who.ddccverifier.trust.pathcheck

import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import org.who.ddccverifier.trust.TrustRegistry

class PCFTrustRegistryTest {
    companion object {
        var registry = PCFTrustRegistry()
        @BeforeClass @JvmStatic fun setup() {
            registry.init(PCFTrustRegistry.PRODUCTION_REGISTRY, PCFTrustRegistry.ACCEPTANCE_REGISTRY)
        }
    }

    @Test
    fun loadEntity() {
        val t = registry.resolve(TrustRegistry.Framework.DCC, "G3jDFQ1oK0Q=")
        assertNotNull(t)
    }
}