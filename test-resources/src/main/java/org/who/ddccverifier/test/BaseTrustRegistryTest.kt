package org.who.ddccverifier.test

import org.junit.BeforeClass
import org.who.ddccverifier.trust.pathcheck.PCFTrustRegistry
import java.io.InputStream
import kotlin.system.measureTimeMillis

open class BaseTrustRegistryTest {
    companion object {
        var registry = PCFTrustRegistry()
        @BeforeClass
        @JvmStatic fun setup() {
            val elapsed = measureTimeMillis {
                registry.init(PCFTrustRegistry.PRODUCTION_REGISTRY, PCFTrustRegistry.ACCEPTANCE_REGISTRY)
            }
            println("Registry Loaded in $elapsed milliseconds")
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