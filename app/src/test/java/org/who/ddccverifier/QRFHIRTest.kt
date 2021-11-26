package org.who.ddccverifier

import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.DDCCVerifier
import ca.uhn.fhir.context.FhirContext
import org.who.ddccverifier.services.CBOR2FHIR
import com.fasterxml.jackson.databind.ObjectMapper

class QRFHIRTest {

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
        val qr1 = open("QR1Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        jsonEquals(open("QR1Unpacked.json"), verified.contents.toString())

        val composition = CBOR2FHIR().run(verified.contents!!)
        val json = jsonParser.encodeResourceToString(composition)

        jsonEquals(open("QR1FHIRComposition.json"), json)
    }

    @Test
    fun fhirQR2() {
        val qr2 = open("QR2Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        jsonEquals(open("QR2Unpacked.json"), verified.contents.toString().replace(": undefined", ": null"))

        val composition = CBOR2FHIR().run(verified.contents!!)
        val json = jsonParser.encodeResourceToString(composition)

        jsonEquals(open("QR2FHIRComposition.json"), json)
    }
}