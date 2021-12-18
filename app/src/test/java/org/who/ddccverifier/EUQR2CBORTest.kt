package org.who.ddccverifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.upokecenter.cbor.CBORNumber
import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.qrs.hcert.HCERTVerifier

class EUQR2CBORTest {
    private val mapper = ObjectMapper()

    private fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun unpackEUQR1() {
        val qr1 = open("EUQR1Contents.txt")
        val CWT = HCERTVerifier().unpack(qr1)
        assertNotNull(CWT)

        val DCC = CWT!![-260][1]
        jsonEquals(open("EUQR1Unpacked.json"), DCC.toString())
    }
}