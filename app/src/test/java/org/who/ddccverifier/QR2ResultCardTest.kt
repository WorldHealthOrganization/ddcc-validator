package org.who.ddccverifier

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.DDCCFormatter
import org.who.ddccverifier.services.DDCCVerifier

class QR2ResultCardTest {

    private val mapper = ObjectMapper()

    private fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun cardResultBuilderQR1() {
        val qr1 = open("QR1Contents.txt")

        val verified = DDCCVerifier().unpackAndVerify(qr1)
        val card = DDCCFormatter().run(verified.contents!!)

        assertNotNull(card)
    }

    @Test
    fun cardResultBuilderQR2() {
        val qr2 = open("QR2Contents.txt")

        val verified = DDCCVerifier().unpackAndVerify(qr2)
        val card = DDCCFormatter().run(verified.contents!!)
        assertNotNull(card)
    }
}