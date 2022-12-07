package org.who.ddccverifier.verify

import com.google.common.truth.Truth.assertThat
import org.hl7.fhir.r4.model.StringType
import org.junit.Test

class FhirLogicalTest {
    class MyDCCLogicalModel(
        var givenName: StringType? = null,
        var familyName: StringType? = null
    ): BaseModel()

    @Test
    fun testGetProperty() {
        val obj = MyDCCLogicalModel(StringType("John"), StringType("Smith"))

        assertThat(
            obj.getProperty("givenName".hashCode(), "givenName", false).first().toString()
        ).isEqualTo("John")

        assertThat(
            obj.getProperty("familyName".hashCode(), "familyName", false).first().toString()
        ).isEqualTo("Smith")
    }
}