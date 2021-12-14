package org.who.ddccverifier

import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.Composition
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.who.ddccverifier.services.CBOR2FHIR
import org.who.ddccverifier.services.CQLEvaluator
import org.who.ddccverifier.services.DDCCVerifier
import org.who.ddccverifier.services.FHIRLibraryLoader
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class CQLEvaluatorAndroidTest {

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val cqlEvaluator = CQLEvaluator(FHIRLibraryLoader(::inputStream))
    private val ddccPass = VersionedIdentifier().withId("DDCCPass").withVersion("0.0.1")

    private fun inputStream(assetName: String): InputStream? {
        return javaClass.classLoader?.getResourceAsStream(assetName)
    }

    private fun open(assetName: String): String {
        return inputStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun evaluateQR1DDCCJSONTest() {
        val asset = jSONParser.parseResource(open("QR1FHIRComposition.json")) as Composition
        Assert.assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(ddccPass, asset, fhirContext)

        Assert.assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }


    @Test
    fun evaluateQR2DDCCJSONTest() {
        val asset = jSONParser.parseResource(open("QR2FHIRComposition.json")) as Composition
        Assert.assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(ddccPass, asset, fhirContext)

        Assert.assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }

    @Test
    fun evaluateQR1DDCCQRTest() {
        val qr1 = open("QR1Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr1)

        Assert.assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        val composition = CBOR2FHIR().run(verified.contents!!)
        val context = cqlEvaluator.run(ddccPass, composition, fhirContext)

        Assert.assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }

    @Test
    fun evaluateQR2DDCCQRTest() {
        val qr1 = open("QR2Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr1)

        Assert.assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)

        val composition = CBOR2FHIR().run(verified.contents!!)
        val context = cqlEvaluator.run(ddccPass, composition, fhirContext)

        Assert.assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }
}