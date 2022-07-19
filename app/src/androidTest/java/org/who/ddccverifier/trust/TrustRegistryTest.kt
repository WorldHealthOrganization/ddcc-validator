package org.who.ddccverifier.trust

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.who.ddccverifier.services.trust.TrustRegistry

class TrustRegistryTest {
    @Test
    fun loadEntity() {
        val t = TrustRegistry.resolve(TrustRegistry.Framework.DCC, "G3jDFQ1oK0Q=");
        assertNotNull(t);
    }
}