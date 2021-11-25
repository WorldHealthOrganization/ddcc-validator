package org.who.ddccverifier

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.who.ddccverifier.services.DDCCVerifier
import androidx.test.platform.app.InstrumentationRegistry
import ca.uhn.fhir.context.FhirContext
import org.who.ddccverifier.services.CBOR2FHIR
import com.fasterxml.jackson.databind.ObjectMapper




@RunWith(AndroidJUnit4::class)
class QR2FHIRTest {

    val mapper = ObjectMapper();

    fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    fun open(assetName: String): String {
        return InstrumentationRegistry.getInstrumentation().context.assets.open(assetName)
            .bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.who.ddccverifier", appContext.packageName)
    }

    @Test
    fun unpackAndVerifyQR1() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val qr1 = open("QR1Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr1)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        jsonEquals(open("QR1Unpacked.json"), verified.contents.toString())

        val bundle = CBOR2FHIR().run(verified.contents!!, appContext)
        val json = FhirContext.forR4().newJsonParser().encodeResourceToString(bundle)

        jsonEquals(open("QR1FHIRBundle.json"), json)
    }

    @Test
    fun unpackAndVerifyQR2() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val qr2 = open("QR2Contents.txt")

        val verified = DDCCVerifier().unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        jsonEquals(open("QR2Unpacked.json"), verified.contents.toString().replace(" undefined,", "null,"))

        val bundle = CBOR2FHIR().run(verified.contents!!, appContext)
        val json = FhirContext.forR4().newJsonParser().encodeResourceToString(bundle)

        jsonEquals(open("QR2FHIRBundle.json"), json)
    }
}