package org.who.ddccverifier.trust

open abstract class TrustRegistryProvider {
    abstract fun create(): TrustRegistry
}