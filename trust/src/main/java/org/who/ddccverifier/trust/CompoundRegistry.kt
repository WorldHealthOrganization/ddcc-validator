package org.who.ddccverifier.trust

class CompoundRegistry(val registries: List<TrustRegistry>): TrustRegistry {
    override fun init() {
        registries.forEach {
            it.init()
        }
    }

    override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {
        registries.forEach {
            it.init(*customRegistries)
        }
    }

    override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? {
        return registries.firstNotNullOfOrNull {
            it.resolve(framework, kid)
        }
    }
}