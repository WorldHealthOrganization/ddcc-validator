package org.who.ddccverifier

import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader
import org.who.ddccverifier.services.qrs.hcert.HCertVerifier
import org.who.ddccverifier.services.fhir.CQLEvaluator
import org.who.ddccverifier.services.fhir.FHIRLibraryLoader
import org.who.ddccverifier.services.qrs.QRUnpacker
import org.who.ddccverifier.services.trust.TrustRegistry
import java.io.InputStream
import java.io.StringReader

@RunWith(AndroidJUnit4::class)
class CQLEvaluatorAndroidTest {

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private val cqlEvaluator = CQLEvaluator(FHIRLibraryLoader(::inputStream))
    private val ddccPass = VersionedIdentifier().withId("DDCCPass").withVersion("0.0.1")

    private fun inputStream(assetName: String): InputStream? {
        return javaClass.classLoader?.getResourceAsStream(assetName)
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            TrustRegistry.init()
        }
    }

    private fun open(assetName: String): String {
        return inputStream(assetName)?.bufferedReader()
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun evaluateDDCCPassOnWHOQR1FromCompositonTest() {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRComposition.json")) as Composition
        Assert.assertEquals("Composition/US111222333444555666", asset.id)

        val context = cqlEvaluator.run(ddccPass, asset)

        Assert.assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }


    @Test
    fun evaluateDDCCPassOnWHOQR2FromCompositonTest() {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRComposition.json")) as Composition
        Assert.assertEquals("Composition/111000111", asset.id)

        val context = cqlEvaluator.run(ddccPass, asset)

        Assert.assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }

    @Test
    fun evaluateDDCCPassOnWHOQR1FromQRTest() {
        val qr1 = open("WHOQR1Contents.txt")
        val verified = HCertVerifier().unpackAndVerify(qr1)

        Assert.assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val context = cqlEvaluator.run(ddccPass, verified.contents!!)

        Assert.assertEquals(false, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }

    @Test
    fun evaluateDDCCPassOnWHOQR2FromQRTest() {
        val qr1 = open("WHOQR2Contents.txt")
        val verified = HCertVerifier().unpackAndVerify(qr1)

        Assert.assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val context = cqlEvaluator.run(ddccPass, verified.contents!!)

        Assert.assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }

    @Test
    fun evaluateDDCCPassOnEUQR1FromQRTest() {
        val qr1 = open("EUQR1Contents.txt")
        val verified = HCertVerifier().unpackAndVerify(qr1)

        Assert.assertEquals(QRUnpacker.Status.VERIFIED, verified.status)

        val context = cqlEvaluator.run(ddccPass, verified.contents!!)

        Assert.assertEquals(true, context.resolveExpressionRef("CompletedImmunization").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("GetFinalDose").evaluate(context))
        Assert.assertNotNull(context.resolveExpressionRef("GetSingleDose").evaluate(context))
    }

    @Test
    fun evaluateHypertensivePatientCQL() {
        val assetBundle = jSONParser.parseResource(open("LibraryTestPatient.json")) as Bundle
        Assert.assertEquals("48d1906f-82df-44d2-9d26-284045504ba9", assetBundle.id)

        val lib = JsonCqlLibraryReader.read(StringReader(open("LibraryTestRules.json")))
        val context = cqlEvaluator.run(lib, assetBundle)

        Assert.assertEquals(true, context.resolveExpressionRef("AgeRange-548").evaluate(context))
        Assert.assertEquals(true, context.resolveExpressionRef("Essential hypertension (disorder)").evaluate(context))
        Assert.assertEquals(false, context.resolveExpressionRef("Malignant hypertensive chronic kidney disease (disorder)").evaluate(context))
        Assert.assertEquals(true, context.resolveExpressionRef("MeetsInclusionCriteria").evaluate(context))
        Assert.assertEquals(false, context.resolveExpressionRef("MeetsExclusionCriteria").evaluate(context))
        Assert.assertEquals(true, context.resolveExpressionRef("InPopulation").evaluate(context))
        Assert.assertEquals("", context.resolveExpressionRef("Recommendation").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("Rationale").evaluate(context))
        Assert.assertNull(context.resolveExpressionRef("Errors").evaluate(context))
    }
}