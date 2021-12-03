package org.who.ddccverifier

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import junit.framework.Assert.assertTrue
import org.hl7.fhir.r4.model.Composition
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.who.ddccverifier.services.CQLEvaluator

@RunWith(AndroidJUnit4::class)
class CQLEvaluatorAndroidTest {

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val cqlEvaluator = CQLEvaluator()

    private fun open(assetName: String): String {
        return javaClass.classLoader?.getResourceAsStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun baseTest() {
        assertTrue(true);
    }

    @Test
    fun evaluateQR1DDCCJSONTest() {
        val asset = jSONParser.parseResource(open("QR1FHIRComposition.json")) as Composition
        Assert.assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(open("DDCCPass.json"), asset, fhirContext)

        Assert.assertEquals(false,
            context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertEquals(null, context.resolveExpressionRef("GetFinalDose").evaluate(context))
    }
}