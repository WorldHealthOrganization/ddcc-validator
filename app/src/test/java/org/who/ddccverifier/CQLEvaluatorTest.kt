package org.who.ddccverifier

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.Bundle
import org.junit.Test
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.annotation.JsonInclude
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.fhir.ucum.UcumEssenceService
import org.junit.Assert.*
import org.hl7.fhir.r4.model.Composition
import org.who.ddccverifier.services.CQLEvaluator
import java.lang.IllegalArgumentException

class CQLEvaluatorTest {

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val cqlEvaluator = CQLEvaluator()

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    /**
     * Translate CQL to Json
     */
    private fun toJson(cqlText: String): String {
        val modelManager = ModelManager()
        val libraryManager = LibraryManager(modelManager).apply {
            librarySourceLoader.registerProvider(FhirLibrarySourceProvider())
        }

        val ucumService = UcumEssenceService(UcumEssenceService::class.java.getResourceAsStream("/ucum-essence.xml"))

        val translator = CqlTranslator.fromText(cqlText, modelManager, libraryManager, ucumService)
        CqlTranslator.getJxsonMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (translator.errors.size > 0) {
            System.err.println("Translation failed due to errors:")
            val errors: ArrayList<String> = ArrayList()
            for (error in translator.errors) {
                val tb = error.locator
                val lines = if (tb == null) "[n/a]" else String.format("[%d:%d, %d:%d]",
                    tb.startLine, tb.startChar, tb.endLine, tb.endChar)
                System.err.printf("%s %s%n", lines, error.message)
                errors.add(lines + error.message)
            }
            throw IllegalArgumentException(errors.toString())
        }

        return translator.toJxson()
    }

    @Test
    fun evaluateHypertensivePatientCQL() {
        val assetBundle = jSONParser.parseResource(open("LibraryTestPatient.json")) as Bundle
        assertEquals("48d1906f-82df-44d2-9d26-284045504ba9", assetBundle.id)

        val context = cqlEvaluator.run(toJson(open("LibraryTestRules.cql")), assetBundle, fhirContext)

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

        val context = cqlEvaluator.run(toJson(open("DDCCPass.cql")), asset, fhirContext)

        assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(false, CQLEvaluator().resolve("CompletedImmunization", open("DDCCPass.json"), asset, FhirContext.forCached(FhirVersionEnum.R4)))
    }

    @Test
    fun evaluateQR1DDCCJSON() {
        val asset = jSONParser.parseResource(open("QR1FHIRComposition.json")) as Composition
        assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.json"), asset, fhirContext)

        assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(false, CQLEvaluator().resolve("CompletedImmunization", open("DDCCPass.json"), asset, FhirContext.forCached(FhirVersionEnum.R4)))
    }

    @Test
    fun evaluateQR2DDCCCQL() {
        val asset = jSONParser.parseResource(open("QR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(toJson(open("DDCCPass.cql")), asset, fhirContext)

        assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertNull( context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(true, CQLEvaluator().resolve("CompletedImmunization", open("DDCCPass.json"), asset, FhirContext.forCached(FhirVersionEnum.R4)))
    }

    @Test
    fun evaluateQR2DDCCJSON() {
        val asset = jSONParser.parseResource(open("QR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.json"), asset, fhirContext)

        assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(true, CQLEvaluator().resolve("CompletedImmunization", open("DDCCPass.json"), asset, FhirContext.forCached(FhirVersionEnum.R4)))
    }
}