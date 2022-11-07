package org.who.ddccverifier

import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** A [TestRule] that cleans up [FhirEngineProvider] instance after each test run. */
class FhirEngineProviderTestRule : TestRule {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        FhirEngineProvider.init(FhirEngineConfiguration(testMode = true))
        try {
          base.evaluate()
        } finally {
          FhirEngineProvider.cleanup()
        }
      }
    }
  }
}