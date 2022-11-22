package org.who.ddccverifier.trust

import java.util.*

object TrustRegistryFactory {
    fun providers(refresh: Boolean): ServiceLoader<TrustRegistryProvider> {
        val loader = ServiceLoader.load<TrustRegistryProvider>(TrustRegistryProvider::class.java)
        if (refresh) {
            loader.reload()
        }
        return loader
    }

    fun getTrustRegistries(): List<TrustRegistry> {
        return providers(false).map {
            it.create()
        }.ifEmpty {
            throw RuntimeException(java.lang.String.join(" ",
                "No TrustRegistryProviders found on the classpath.",
                "You need to add a reference to one of the 'trust-didweb' or 'trust-pathcheck' packages,",
                "or provide your own implementation."))
        }
    }
}