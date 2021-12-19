package org.who.ddccverifier

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r4.model.Bundle
import org.junit.Test
import ca.uhn.fhir.context.FhirVersionEnum
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.fhir.ucum.UcumEssenceService
import org.junit.Assert.*
import org.hl7.fhir.r4.model.Composition
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader
import org.who.ddccverifier.services.fhir.CQLEvaluator
import org.who.ddccverifier.services.fhir.FHIRLibraryLoader
import java.io.StringReader
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

class CQLEvaluatorTest: BaseTest() {

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val ddccPass = VersionedIdentifier().withId("DDCCPass").withVersion("0.0.1")

    private val cqlEvaluator = CQLEvaluator(FHIRLibraryLoader(::inputStream))


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
    fun evaluateHypertensivePatientFromCQLTest() {
        val assetBundle = jSONParser.parseResource(open("LibraryTestPatient.json")) as Bundle
        assertEquals("48d1906f-82df-44d2-9d26-284045504ba9", assetBundle.id)

        val lib = JsonCqlLibraryReader.read(StringReader(toJson(open("LibraryTestRules.cql"))))
        val context = cqlEvaluator.run(lib, assetBundle)

        assertEquals(true, context.resolveExpressionRef("AgeRange-548").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("Essential hypertension (disorder)").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("Malignant hypertensive chronic kidney disease (disorder)").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("MeetsInclusionCriteria").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("MeetsExclusionCriteria").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("InPopulation").evaluate(context))
        assertEquals("", context.resolveExpressionRef("Recommendation").evaluate(context))
        assertEquals(null, context.resolveExpressionRef("Rationale").evaluate(context))
        assertEquals(null, context.resolveExpressionRef("Errors").evaluate(context))
    }

    @Test
    fun evaluateDDCCPassAsCQLOnQR1FromCompositionTest() {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRComposition.json")) as Composition
        assertEquals("Composition/US111222333444555666", asset.id)

        val lib = JsonCqlLibraryReader.read(StringReader(toJson(open("DDCCPass.cql"))))
        val context = cqlEvaluator.run(lib, asset)

        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetAllModerna").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("ModernaProtocol").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(false, cqlEvaluator.resolve("CompletedImmunization", ddccPass, asset))
    }

    @Test
    fun evaluateDDCCPassAsJSONOnQR1FromCompositionTest() {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRComposition.json")) as Composition
        assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(ddccPass, asset)

        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(false, cqlEvaluator.resolve("CompletedImmunization", ddccPass, asset))
    }

    @Test
    fun evaluateDDCCPassAsCQLOnQR2FromCompositionTest() {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val lib = JsonCqlLibraryReader.read(StringReader(toJson(open("DDCCPass.cql"))))
        val context = cqlEvaluator.run(lib, asset)

        assertNotEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertEquals(Collections.EMPTY_LIST,  context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(true, cqlEvaluator.resolve("CompletedImmunization", ddccPass, asset))
    }

    @Test
    fun evaluateDDCCPassAsJSONOnQR2FromCompositionTest() {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRComposition.json")) as Composition
        assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(ddccPass, asset)

        assertNotEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(true, cqlEvaluator.resolve("CompletedImmunization", ddccPass, asset))
    }

    @Test
    fun evaluateDDCCPassAsCQLOnSHCQR1FromCompositionTest() {
        val asset = jSONParser.parseResource(open("SHCQR1FHIRComposition.json")) as Composition

        val lib = JsonCqlLibraryReader.read(StringReader(toJson(open("DDCCPass.cql"))))
        val context = cqlEvaluator.run(lib, asset)

        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetSingleDose").evaluate(context))
        assertEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetFinalDose").evaluate(context))
        assertNotEquals(Collections.EMPTY_LIST, context.resolveExpressionRef("GetAllModerna").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("ModernaProtocol").evaluate(context))
        assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        assertEquals(true, cqlEvaluator.resolve("CompletedImmunization", ddccPass, asset))
    }
}