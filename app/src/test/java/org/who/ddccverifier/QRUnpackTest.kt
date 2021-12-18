package org.who.ddccverifier

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.qrs.hcert.HCertVerifier

class QRUnpackTest {
    private val mapper = ObjectMapper()

    private fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun unpackWHOQR1() {
        val qr1 = open("WHOQR1Contents.txt")
        val verified = HCertVerifier().unpack(qr1)
        assertNotNull(verified)
        jsonEquals(open("WHOQR1Unpacked.json"), verified.toString())
    }

    @Test
    fun unpackWHOQR2() {
        val qr2 = open("WHOQR2Contents.txt")
        val verified = HCertVerifier().unpack(qr2)
        assertNotNull(verified)
        jsonEquals(open("WHOQR2Unpacked.json"), verified.toString().replace(": undefined", ": null"))
    }

    @Test
    fun unpackEUQR1() {
        val qr1 = open("EUQR1Contents.txt")
        val CWT = HCertVerifier().unpack(qr1)
        assertNotNull(CWT)

        val DCC = CWT!![-260][1]
        jsonEquals(open("EUQR1Unpacked.json"), DCC.toString())
    }
}