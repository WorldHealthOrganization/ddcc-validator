package org.who.ddccverifier.verify

import org.junit.BeforeClass
import org.who.ddccverifier.trust.pathcheck.PCFTrustRegistry
import java.io.InputStream

open class BaseTest {
    companion object {
        var registry = PCFTrustRegistry()
        @BeforeClass
        @JvmStatic fun setup() {
            registry.init()
            registry.addTestKeys()
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