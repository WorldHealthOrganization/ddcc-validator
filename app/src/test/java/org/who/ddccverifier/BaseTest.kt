package org.who.ddccverifier

import java.io.InputStream

open class BaseTest {
    fun inputStream(assetName: String): InputStream? {
        return javaClass.classLoader?.getResourceAsStream(assetName)
    }

    fun open(assetName: String): String {
        return inputStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }
}