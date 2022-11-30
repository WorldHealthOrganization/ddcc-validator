package org.who.ddccverifier

import androidx.test.core.app.ApplicationProvider
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.who.ddccverifier.test.BaseTrustRegistryTest
import org.who.ddccverifier.services.cql.CqlBuilder
import org.who.ddccverifier.services.cql.FhirOperator
import java.util.*
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class CQLEvaluatorTest: BaseTrustRegistryTest() {
    @get:Rule
    val fhirEngineProviderRule = FhirEngineProviderTestRule()

    private val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
    private val jSONParser = fhirContext.newJsonParser()

    private lateinit var fhirEngine: FhirEngine
    private lateinit var fhirOperator: FhirOperator

    private val ddccPass = CqlBuilder.compileAndBuild(inputStream("TestPass-1.0.0.cql")!!)

    @Before
    fun setUp() = runBlocking {
        val elapsed = measureTimeMillis {
            fhirEngine = FhirEngineProvider.getInstance(ApplicationProvider.getApplicationContext())
            fhirOperator = FhirOperator(fhirContext, fhirEngine)
            fhirOperator.loadLib(ddccPass)

            TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        }
        println("TIME: Test Initialized in $elapsed")
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
    fun evaluateTestPassAsCQLOnQR1FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetAllModerna"))
        assertEquals(false, results.getParameterBool("ModernaProtocol"))
        assertEquals(false, results.getParameterBool("CompletedImmunization"))
    }

    @Test
    fun evaluateTestPassAsJSONOnQR1FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertEquals(false, results.getParameterBool("CompletedImmunization"))
    }

    @Test
    fun evaluateTestPassAsCQLOnQR2FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST,  results.getParameters("GetFinalDose"))
        assertEquals(true, results.getParameterBool("CompletedImmunization"))
    }

    @Test
    fun evaluateTestPassAsJSONOnQR2FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("WHOQR2FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertEquals(true, results.getParameterBool("CompletedImmunization"))
    }

    @Test
    fun evaluateTestPassAsCQLOnSHCQR1FromBundleTest() = runBlocking {
        val asset = jSONParser.parseResource(open("SHCQR1FHIRBundle.json")) as Bundle

        loadBundle(asset)

        val results = fhirOperator.evaluateLibrary(
            "http://localhost/Library/TestPass|1.0.0",
            patId(asset),
            setOf("CompletedImmunization", "GetFinalDose", "GetSingleDose",
                "GetAllModerna", "ModernaProtocol")) as Parameters

        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetSingleDose"))
        assertEquals(Collections.EMPTY_LIST, results.getParameters("GetFinalDose"))
        assertNotEquals(Collections.EMPTY_LIST, results.getParameters("GetAllModerna"))
        assertEquals(true, results.getParameterBool("ModernaProtocol"))
        assertEquals(true, results.getParameterBool("CompletedImmunization"))
    }
}