package org.who.ddccverifier

import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.system.measureTimeMillis

/** A [TestRule] that cleans up [FhirEngineProvider] instance after each test run. */
class FhirEngineProviderTestRule : TestRule {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        val elapsedInit = measureTimeMillis {
          FhirEngineProvider.init(FhirEngineConfiguration(testMode = true))
        }
        println("TIME: FhirEngineProvider Loaded in $elapsedInit")

        try {
          base.evaluate()
        } finally {
          val elapsedCleanup = measureTimeMillis {
            FhirEngineProvider.cleanup()
          }
          println("TIME: FhirEngineProvider Cleaned in $elapsedCleanup")
        }
      }
    }
  }
}