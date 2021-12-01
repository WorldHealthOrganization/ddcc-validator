package org.who.ddccverifier

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.Bundle
import org.junit.Test
import ca.uhn.fhir.context.FhirVersionEnum
import org.junit.Assert.*
import org.hl7.fhir.r4.model.Composition
import org.who.ddccverifier.services.CQLEvaluator

class CQLEvaluatorTest {

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val cqlEvaluator = CQLEvaluator()

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun evaluateHypertensivePatientCQL() {
        val assetBundle = jSONParser.parseResource(open("LibraryTestPatient.json")) as Bundle
        assertEquals("48d1906f-82df-44d2-9d26-284045504ba9", assetBundle.id)

        val context = cqlEvaluator.run(open("LibraryTestRules.cql"), assetBundle, fhirContext)

        assertEquals(true, context.resolveExpressionRef("AgeRange-548").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("Essential hypertension (disorder)").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("Malignant hypertensive chronic kidney disease (disorder)").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("MeetsInclusionCriteria").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("MeetsExclusionCriteria").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("InPopulation").evaluate(context))
        assertEquals("", context.resolveExpressionRef("Recommendation").evaluate(context))
        assertNull(context.resolveExpressionRef("Rationale").evaluate(context))
        assertNull(context.resolveExpressionRef("Errors").evaluate(context))
    }

    @Test
    fun evaluateHypertensivePatientXML() {
        val assetBundle = jSONParser.parseResource(open("LibraryTestPatient.json")) as Bundle
        assertEquals("48d1906f-82df-44d2-9d26-284045504ba9", assetBundle.id)

        val context = cqlEvaluator.run(open("LibraryTestRules.xml"), assetBundle, fhirContext)

        assertEquals(true, context.resolveExpressionRef("AgeRange-548").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("Essential hypertension (disorder)").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("Malignant hypertensive chronic kidney disease (disorder)").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("MeetsInclusionCriteria").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("MeetsExclusionCriteria").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("InPopulation").evaluate(context))
        assertEquals("", context.resolveExpressionRef("Recommendation").evaluate(context))
        assertNull(context.resolveExpressionRef("Rationale").evaluate(context))
        assertNull(context.resolveExpressionRef("Errors").evaluate(context))
    }

    @Test
    fun evaluateQR1DDCCCQL() {
        val asset = jSONParser.parseResource(open("QR1FHIRComposition.json")) as Composition
        assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.cql"), asset, fhirContext)

        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))
    }

    @Test
    fun evaluateQR1DDCCXML() {
        val asset = jSONParser.parseResource(open("QR1FHIRComposition.json")) as Composition
        assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.xml"), asset, fhirContext)

        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))
    }

    @Test
    fun evaluateQR1DDCCJSON() {
        val asset = jSONParser.parseResource(open("QR1FHIRComposition.json")) as Composition
        assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.json"), asset, fhirContext)

        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))
    }

    @Test
    fun evaluateQR2DDCCCQL() {
        val asset = jSONParser.parseResource(open("QR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.cql"), asset, fhirContext)

        assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertNull( context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
    }

    @Test
    fun evaluateQR2DDCCXML() {
        val asset = jSONParser.parseResource(open("QR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.xml"), asset, fhirContext)

        assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
    }

    @Test
    fun evaluateQR2DDCCJSON() {
        val asset = jSONParser.parseResource(open("QR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.json"), asset, fhirContext)

        assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
    }
}