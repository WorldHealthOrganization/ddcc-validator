package org.who.ddccverifier.verify

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.who.ddccverifier.QRDecoder
import org.who.ddccverifier.test.BaseTrustRegistryTest
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class QRVerifyTest: BaseTrustRegistryTest() {

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


    @Before
    fun setUp() {
        // fixes timezone for testing
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    private val mapper = jacksonObjectMapper()
    private val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

    private fun jsonEquals(v1: String, v2: String) {
        assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    @OptIn(ExperimentalTime::class)
    fun verify(qrContentsFileName: String, expectedJsonFileName: String, expectedStatus: QRDecoder.Status = QRDecoder.Status.VERIFIED) {
        Mockito.mockStatic(UUID::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { UUID.randomUUID() }.thenReturn(firstUUID, *uuidList)
            val qr1 = open(qrContentsFileName)

            val (verified, elapsed) = measureTimedValue {
                QRDecoder(registry).decode(qr1)
            }
            println("TIME: UnpackAndVerify in $elapsed")

            assertNotNull(verified)
            assertEquals(expectedStatus, verified.status)

            val json = jsonParser.encodeResourceToString(verified.contents!!)

            jsonEquals(open(expectedJsonFileName), json)
        }
    }

    @Test
    fun verifyWHOQR1() {
        verify("WHOQR1Contents.txt", "WHOQR1FHIRBundle.json")
    }

    @Test
    fun verifyWHOQR2() {
        verify("WHOQR2Contents.txt", "WHOQR2FHIRBundle.json")
    }

    @Test
    fun verifyWHOSingaporePCR() {
        verify("WHOSingaporePCRContents.txt", "WHOSingaporePCRFHIRBundle.json")
    }

    @Test
    fun verifyWHOEUQR1() {
        verify("EUQR1Contents.txt", "EUQR1FHIRBundle.json")
    }

    @Test
    fun verifyDCCItalyAcceptanceQR() {
        verify("EUItalyAcceptanceQRContents.txt", "EUItalyAcceptanceQRFHIRBundle.json")
    }

    @Test
    fun verifyDCCIndonesia() {
        verify("EUIndonesiaContents.txt", "EUIndonesiaFHIRBundle.json")
    }

    @Test
    fun verifyDCCUruguay() {
        verify("EUUruguayContents.txt", "EUUruguayFHIRBundle.json", QRDecoder.Status.ISSUER_NOT_TRUSTED)
    }

    @Test
    fun verifyDCCColombia() {
        verify("EUColombiaContents.txt", "EUColombiaFHIRBundle.json", QRDecoder.Status.ISSUER_NOT_TRUSTED)
    }

    @Test
    fun verifySHCQR1() {
        verify("SHCQR1Contents.txt", "SHCQR1FHIRBundle.json")
    }

    @Test
    fun verifySHCSenegal() {
        verify("SHCSenegalContents.txt", "SHCSenegalFHIRBundle.json")
    }

    @Test
    fun verifySHCTestResults() {
        verify("SHCTestResultsContents.txt", "SHCTestResultsFHIRBundle.json")
    }

    @Test
    fun verifySHCWAVaxTestResults() {
        verify("SHCWAVaxContents.txt", "SHCWAVaxFHIRBundle.json")
    }

    @Test
    fun verifyDIVOCQR() {
        verify("DIVOCQR1Contents.txt", "DIVOCQR1FHIRBundle.json")
    }

    @Test
    fun verifyDIVOCJamaica() {
        verify("DIVOCJamaicaContents.txt", "DIVOCJamaicaFHIRBundle.json", QRDecoder.Status.INVALID_SIGNATURE)
    }

    @Test
    fun verifyDIVOCIndonesia() {
        verify("DIVOCIndonesiaContents.txt", "DIVOCIndonesiaFHIRBundle.json")
    }

    @Test
    fun verifyICAOAustraliaQR1() {
        verify("ICAOAUQR1Contents.txt", "ICAOAUQR1FHIRBundle.json")
    }

    @Test
    fun verifyICAOJapanQR1() {
        verify("ICAOJPQR1Contents.txt", "ICAOJPQR1FHIRBundle.json")
    }
}