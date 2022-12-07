package org.who.ddccverifier.trust.didweb

import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import org.who.ddccverifier.trust.TrustRegistry

class DDCCTrustRegistryTest {
    companion object {
        var registry = DDCCTrustRegistry()
        @BeforeClass @JvmStatic fun setup() {
            registry.init(DDCCTrustRegistry.PRODUCTION_REGISTRY, DDCCTrustRegistry.ACCEPTANCE_REGISTRY)
        }
    }

    @Test
    fun loadEntity() {
        val t = registry.resolve(TrustRegistry.Framework.DIVOC, "india")
        assertNotNull(t)
    }

    @Test
    fun testWASHC() {
        val t = registry.resolve(TrustRegistry.Framework.DCC, "https://waverify.doh.wa.gov/creds#n0S0H6_mbA93e3pEu-a67qoiF4CAWYsOGoWo6TLHUzQ")
        assertNotNull(t)
    }
}
    @Test
    fun testDCCItalyAcceptance() {
        val t = registry.resolve(TrustRegistry.Framework.DCC, "OTAXaM3aBRM=")
        assertNotNull(t)
    }
