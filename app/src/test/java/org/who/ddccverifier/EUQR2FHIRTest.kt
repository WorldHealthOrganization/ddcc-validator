package org.who.ddccverifier

import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.qrs.hcert.HCERTVerifier
import ca.uhn.fhir.context.FhirContext
import org.who.ddccverifier.services.qrs.hcert.WHOCBOR2FHIR
import com.fasterxml.jackson.databind.ObjectMapper
import org.who.ddccverifier.services.qrs.QRUnpacker

class EUQR2FHIRTest {

    private val mapper = ObjectMapper()
    private val jsonParser = FhirContext.forR4().newJsonParser()

    private fun jsonEquals(v1: String, v2: String) {
        assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun fhirQR1() {
        val qr1 = open("EUQR1Contents.txt")
        val verified = HCERTVerifier().unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("EUQR1FHIRComposition.json"), json)
    }
}