package org.who.ddccverifier

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.CBOR2FHIR
import org.who.ddccverifier.services.CQLEvaluator
import org.who.ddccverifier.services.DDCCFormatter
import org.who.ddccverifier.services.DDCCVerifier

class QRResultCardTest {

    private val mapper = ObjectMapper()

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun cardResultBuilderQR1() {
        val qr1 = open("QR1Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr1)

        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        val composition = CBOR2FHIR().run(verified.contents!!)
        val card2 = DDCCFormatter().run(composition)
        val status = CQLEvaluator().resolve(
            "CompletedImmunization", open("DDCCPass.json"),
            composition, FhirContext.forCached(FhirVersionEnum.R4)) as Boolean

        val context = CQLEvaluator().run(open("DDCCPass.json"), composition, FhirContext.forCached(FhirVersionEnum.R4))
        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))

        // Credential
        assertEquals("COVID-19 Vaccination", card2.cardTitle!!.split(" - ")[1])
        assertEquals("Valid from Jul 8, 2021 to Jul 8, 2022", card2.validUntil)

        // Patient
        assertEquals("Eddie Murphy", card2.personName)
        assertEquals("Sep 19, 1986 - Male", card2.personDetails)
        assertEquals("ID: 1234567890", card2.identifier)

        // Immunization
        assertEquals("SARS-CoV-2 mRNA Vaccine", card2.vaccineType)
        assertEquals("Dose: 1 of 2", card2.dose)
        assertEquals("Jul 8, 2021", card2.doseDate)
        assertEquals("Jul 22, 2021", card2.vaccineValid)
        assertEquals("COVID-19", card2.vaccineAgainst)
        assertEquals("TEST (#PT123F), TEST", card2.vaccineInfo)
        assertEquals("TEST", card2.vaccineInfo2)
        assertEquals("Vaccination Site, USA", card2.location)
        assertEquals("US111222333444555666", card2.hcid)
        assertEquals("wA69g8VD512TfTTdkTNSsG", card2.pha)
        assertEquals("http://www.acme.org/practitioners/23", card2.hw)

        // Recommendation
        assertEquals("Jul 29, 2021", card2.nextDose)

        assertEquals(false, status)
    }

    @Test
    fun cardResultBuilderQR2() {
        val qr2 = open("QR2Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr2)

        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        val composition = CBOR2FHIR().run(verified.contents!!)
        val card2 = DDCCFormatter().run(composition)
        val DDCCPass = open("DDCCPass.json")

        val status = CQLEvaluator().resolve(
            "CompletedImmunization", DDCCPass,
            composition, FhirContext.forCached(FhirVersionEnum.R4)) as Boolean

        // Credential
        assertEquals("COVID-19 Vaccination", card2.cardTitle!!.split(" - ")[1])
        assertEquals(null, card2.validUntil)

        // Patient
        assertEquals("EddieMurphy", card2.personName)
        assertEquals("Sep 19, 1986 - Male", card2.personDetails)
        assertEquals("ID: 111000111", card2.identifier)

        // Immunization
        assertEquals("SARS-CoV-2 mRNA Vaccine", card2.vaccineType)
        assertEquals("Dose: 1", card2.dose)
        assertEquals(null, card2.doseDate)
        assertEquals(null, card2.vaccineValid)
        assertEquals("COVID-19", card2.vaccineAgainst)
        assertEquals("Organization/973 (#PT123F.9)", card2.vaccineInfo)
        assertEquals(null, card2.vaccineInfo2)
        assertEquals("Location/971", card2.location)
        assertEquals("111000111", card2.hcid)
        assertEquals(null, card2.pha)
        assertEquals(null, card2.hw)

        // Recommendation
        assertEquals(null, card2.nextDose)

        assertEquals(true, status)
    }
}