package org.who.ddccverifier.test

import org.who.ddccverifier.trust.pathcheck.PCFTrustRegistry
import java.io.InputStream
import kotlin.system.measureTimeMillis

open class BaseTrustRegistryTest {
    companion object {
        var registry = PCFTrustRegistry()

        init {
            val elapsed = measureTimeMillis {
                registry.init(PCFTrustRegistry.PRODUCTION_REGISTRY, PCFTrustRegistry.ACCEPTANCE_REGISTRY)
            }
            println("TIME: Registry Loaded in $elapsed")
        }
    }

    fun inputStream(assetName: String): InputStream? {
        return javaClass.classLoader?.getResourceAsStream(assetName)
    }

    fun open(assetName: String): String {
        return inputStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }
}