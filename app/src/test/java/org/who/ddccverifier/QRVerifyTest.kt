package org.who.ddccverifier

import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.qrs.hcert.HCertVerifier
import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.who.ddccverifier.services.qrs.QRUnpacker
import org.who.ddccverifier.services.qrs.shc.SHCVerifier
import org.who.ddccverifier.services.qrs.divoc.DivocVerifier


class QRVerifyTest: BaseTest() {

    private val mapper = ObjectMapper()
    private val jsonParser = FhirContext.forR4().newJsonParser()

    private fun jsonEquals(v1: String, v2: String) {
        assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    @Test
    fun verifyWHOQR1() {
        val qr1 = open("WHOQR1Contents.txt")
        val verified = HCertVerifier().unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("WHOQR1FHIRComposition.json"), json)
    }

    @Test
    fun verifyWHOQR2() {
        val qr2 = open("WHOQR2Contents.txt")
        val verified = HCertVerifier().unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("WHOQR2FHIRComposition.json"), json)
    }

    @Test
    fun verifyWHOEUQR1() {
        val qr1 = open("EUQR1Contents.txt")
        val verified = HCertVerifier().unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("EUQR1FHIRComposition.json"), json)
    }

    @Test
    fun verifySHCQR1() {
        val qr1 = open("SHCQR1Contents.txt")
        val verified = SHCVerifier().unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("SHCQR1FHIRComposition.json"), json)
    }

    @Test
    fun verifyDIVOCQR() {
        val qr1 = open("DIVOCQR1Contents.txt")
        val verified = DivocVerifier(::inputStream).unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("DIVOCQR1FHIRComposition.json"), json)
    }
}