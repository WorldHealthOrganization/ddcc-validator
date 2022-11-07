package org.who.ddccverifier

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.get
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.ResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.who.ddccverifier.verify.hcert.HCertVerifier
import org.who.ddccverifier.services.cql.CqlBuilder
import org.who.ddccverifier.services.cql.FhirOperator
import org.who.ddccverifier.verify.divoc.DivocVerifier
import org.who.ddccverifier.verify.icao.IcaoVerifier
import org.who.ddccverifier.verify.shc.ShcVerifier
import java.util.*

@RunWith(AndroidJUnit4::class)
class CQLEvaluatorAndroidTest: BaseTest() {
    @get:Rule
    val fhirEngineProviderRule = FhirEngineProviderTestRule()

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private lateinit var fhirEngine: FhirEngine
    private lateinit var fhirOperator: FhirOperator

    private val ddccPass = CqlBuilder.compileAndBuild(inputStream("DDCCPass-1.0.0.cql")!!)

    @Before
    fun setUp() = runBlocking {
        fhirEngine = FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())
        fhirOperator = FhirOperator(fhirContext, fhirEngine)
        fhirOperator.loadLib(ddccPass)
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    private suspend fun loadBundle(bundle: Bundle?) {
        checkNotNull(bundle)
        for (entry in bundle.entry) {
            when (entry.resource.resourceType) {
                ResourceType.Library -> fhirOperator.loadLib(entry.resource as Library)
                ResourceType.Bundle -> Unit
                else -> fhirEngine.create(entry.resource)
            }
        }
    }

    private fun patId(bundle: Bundle?): String {
        checkNotNull(bundle)
        return bundle.entry.filter { it.resource is Patient }.first().resource.id.removePrefix("Patient/")
    }

    @Test
    fun evaluateDDCCPassOnWHOQR1FromCompositonTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(false, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }


    @Test
    fun evaluateDDCCPassOnWHOQR2FromCompositonTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(true, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }

    @Test
    fun evaluateDDCCPassOnWHOQR1FromQRTest() = runBlocking {
        val qr1 = open("WHOQR1Contents.txt")
        val verified = HCertVerifier(registry).unpackAndVerify(qr1)

        loadBundle(verified.contents)

        Assert.assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(verified.contents),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(false, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }

    @Test
    fun evaluateDDCCPassOnWHOQR2FromQRTest() = runBlocking {
        val qr1 = open("WHOQR2Contents.txt")
        val verified = HCertVerifier(registry).unpackAndVerify(qr1)

        loadBundle(verified.contents)

        Assert.assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(verified.contents),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(true, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }

    @Test
    fun evaluateDDCCPassOnEUQR1FromQRTest() = runBlocking {
        val qr1 = open("EUQR1Contents.txt")
        val verified = HCertVerifier(registry).unpackAndVerify(qr1)

        loadBundle(verified.contents)

        Assert.assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(verified.contents),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(true, results.getParameterBool("CompletedImmunization"))
        Assert.assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }

    @Test
    fun evaluateDDCCPassOnSHCQR1FromQRTest() = runBlocking {
        val qr1 = open("SHCQR1Contents.txt")
        val verified = ShcVerifier(registry).unpackAndVerify(qr1)

        loadBundle(verified.contents)

        Assert.assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(verified.contents),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(true, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }

    @Test
    fun evaluateDDCCPassOnDIVOCQR1FromQRTest() = runBlocking {
        val qr1 = open("DIVOCQR1Contents.txt")
        val verified = DivocVerifier(registry).unpackAndVerify(qr1)

        loadBundle(verified.contents)

        Assert.assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(verified.contents),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(false, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }

    @Test
    fun evaluateDDCCPassOnICAOQR1FromQRTest() = runBlocking {
        val qr1 = open("ICAOAUQR1Contents.txt")
        val verified = IcaoVerifier(registry).unpackAndVerify(qr1)

        loadBundle(verified.contents)

        Assert.assertEquals(QRDecoder.Status.VERIFIED, verified.status)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/DDCCPass|1.0.0",
            patId(verified.contents),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose")) as Parameters

        Assert.assertEquals(true, results.getParameterBool("CompletedImmunization"))
        Assert.assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        Assert.assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
    }
}