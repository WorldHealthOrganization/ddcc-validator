package org.who.ddccverifier.verify

import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import org.who.ddccverifier.verify.divoc.DivocVerifier
import org.who.ddccverifier.verify.hcert.HCertVerifier
import org.who.ddccverifier.verify.icao.IcaoVerifier
import org.who.ddccverifier.verify.shc.SHCVerifier
import java.util.*


class QRVerifyTest: BaseTest() {

    val firstUUID = UUID.fromString("43293785-70d2-4cbe-8ecf-30b947fd45d5")
    val uuidList = listOf<UUID>(
        UUID.fromString("a9152ef1-efdd-4e3e-b8c3-d02153afc059"),
        UUID.fromString("969e8fbe-52b8-425f-8c4c-bbd6f7aa886f"),
        UUID.fromString("d9b35e76-f553-4a18-9254-3372d9ea8c73"),
        UUID.fromString("7c967bcb-abc6-4cb6-b590-7b5827d9aae3"),
        UUID.fromString("04db279a-ba4d-472d-b93e-3e725f3ecbea"),
        UUID.fromString("450c6c0b-cdc4-46e7-8cbd-2ef878d3c9da"),
        UUID.fromString("c18e08d0-b544-4184-8913-cbf7d437bab1"),
        UUID.fromString("4fad3e42-d4fc-4a12-bf25-732ea47aa147"),
        UUID.fromString("6f99f8fe-4964-4f14-b493-a93f61dc3c45"),
        UUID.fromString("0e93cc28-96fb-44ff-a416-db884ae34778"),
        UUID.fromString("1a266e51-a6ec-4d28-963f-9db815a7f085"),
        UUID.fromString("0be7e82a-286a-4c99-8d15-f75cf77ad55a"),
        UUID.fromString("7d8168c8-5f0c-47fa-bab3-b261de37e58f"),
        UUID.fromString("e947d96b-bdba-4ce5-99a2-97b24ec8d857"),
        UUID.fromString("850537e4-3917-468f-9cef-42a69e39d0c7"),
        UUID.fromString("1bbc3717-3e17-4507-9d5d-ba86a5c69572"),
        UUID.fromString("02a9615b-9b31-4e40-b599-bcad51689874"),
        UUID.fromString("8f0ce1de-65ca-41a7-9d35-e6580177ed7a"),
        UUID.fromString("ab7805f4-06d7-43a2-aa54-76458bcf623f")
    ).toTypedArray()

    private val mapper = ObjectMapper()
    private val jsonParser = FhirContext.forR4().newJsonParser().setPrettyPrint(true)

    private fun jsonEquals(v1: String, v2: String) {
        assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    @Test
    fun verifyWHOQR1() {
        val qr1 = open("WHOQR1Contents.txt")
        val verified = HCertVerifier(registry).unpackAndVerify(qr1)

        assertNotNull(verified)
        assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("WHOQR1FHIRComposition.json"), json)
    }

    @Test
    fun verifyWHOQR2() {
        val qr2 = open("WHOQR2Contents.txt")
        val verified = HCertVerifier(registry).unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("WHOQR2FHIRComposition.json"), json)
    }

    @Test
    fun verifyWHOSingaporePCR() {
        val qr2 = open("WHOSingaporePCRContents.txt")
        val verified = HCertVerifier(registry).unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val json = jsonParser.encodeResourceToString(verified.contents!!)

        jsonEquals(open("WHOSingaporePCRFHIRComposition.json"), json)
    }

    @Test
    fun verifyWHOEUQR1() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)

            val qr1 = open("EUQR1Contents.txt")
            val verified = HCertVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("EUQR1FHIRBundle.json"), json)
        }
    }

    @Test
    fun verifyDCCIndonesia() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)

            val qr1 = open("EUIndonesiaContents.txt")
            val verified = HCertVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("EUIndonesiaFHIRBundle.json"), json)
        }
    }

    @Test
    fun verifyDCCUruguay() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("EUUruguayContents.txt")
            val verified = HCertVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.ISSUER_NOT_TRUSTED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("EUUruguayFHIRBundle.json"), json)
        }
    }

    @Test
    fun verifySHCQR1() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("SHCQR1Contents.txt")
            val verified = SHCVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("SHCQR1FHIRBundle.json"), json)
        }
    }

    @Test
    fun verifySHCTestResults() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("SHCTestResultsContents.txt")
            val verified = SHCVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("SHCTestResultsFHIRBundle.json"), json)
        }
    }

    @Test
    fun verifyDIVOCQR() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("DIVOCQR1Contents.txt")
            val verified = DivocVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("DIVOCQR1FHIRComposition.json"), json)
        }
    }

    @Test
    fun verifyDIVOCJamaica() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)

            val qr1 = open("DIVOCJamaicaContents.txt")
            val verified = DivocVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.INVALID_SIGNATURE, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("DIVOCJamaicaFHIRComposition.json"), json)
        }
    }

    @Test
    fun verifyDIVOCIndonesia() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("DIVOCIndonesiaContents.txt")
            val verified = DivocVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("DIVOCIndonesiaFHIRComposition.json"), json)
        }
    }

    @Test
    fun verifyICAOAustraliaQR1() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("ICAOAUQR1Contents.txt")
            val verified = IcaoVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("ICAOAUQR1FHIRBundle.json"), json)
        }
    }

    @Test
    fun verifyICAOJapanQR1() {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open("ICAOJPQR1Contents.txt")
            val verified = IcaoVerifier(registry).unpackAndVerify(qr1)

            assertNotNull(verified)
            assertEquals(QRDecoder.Status.VERIFIED, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open("ICAOJPQR1FHIRBundle.json"), json)
        }
    }
}