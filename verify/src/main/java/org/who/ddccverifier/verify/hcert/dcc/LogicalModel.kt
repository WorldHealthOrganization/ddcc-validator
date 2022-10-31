package org.who.ddccverifier.verify.hcert.dcc.logical

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.utils.FHIRLogical
import org.who.ddccverifier.verify.shc.DecimalToDataTimeDeserializer
import java.util.*

class CWT (
    @JsonProperty("1")
    val iss: StringType?,   // Issuer
    @JsonProperty("2")
    val sub: StringType?,   // Subject
    @JsonProperty("3")
    val aud: StringType?,   // Audience
    @JsonProperty("4")
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val exp: DateTimeType?, // expiration
    @JsonProperty("5")
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val nbf: DateTimeType?, // not before date
    @JsonProperty("6")
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val iat: DateTimeType?, // issued at date
    @JsonProperty("7")
    val id: StringType?,   // Audience
    @JsonProperty("-260")
    val data: HCERT?,      // Certificate
): FHIRLogical()

class HCERT(
    @JsonProperty("1")
    val cert: HC1?          // Cert
): FHIRLogical()

class HC1(
    val ver: StringType?,       // Schema version
    val nam: PersonName?,       // Person name
    val dob: DateType?,         // date of birth
    val v: List<Vaccination>?,  // Vaccination Group
    val t: List<Test>?,         // Test Group
    val r: List<Recovery>?      // Recovery Group
): FHIRLogical()

class PersonName(
    val fn	:	StringType?,  // Surname
    val fnt	:	StringType?,  // Standardised surname
    val gn	:	StringType?,  // Forename
    val gnt	:	StringType?   // Standardised forename
): FHIRLogical()

class Vaccination(
    val tg:	StringType?,    // disease or agent targeted
    val vp:	StringType?,    // vaccine or prophylaxis
    val mp:	StringType?,    // vaccine medicinal product
    val ma:	StringType?,    // Marketing Authorization Holder - if no MAH present, then manufacturer
    val dn: PositiveIntType?,   // Dose Number
    val sd: PositiveIntType?,   // Total Series of Doses
    val dt: DateTimeType?,  // ISO8601 complete date: Date of Vaccination
    val co:	StringType?,    // Country of Vaccination
    val `is`:StringType?,   // Certificate Issuer
    val ci:	StringType?     // Unique Certificate Identifier: UVCI
): FHIRLogical()

class Test(
    val tg:	StringType?,    //disease or agent targeted
    val tt:	StringType?,    //Type of Test
    val nm:	StringType?,    // NAA Test Name
    val ma:	StringType?,    // RAT Test name and manufacturer
    val sc: DateTimeType?,  // Date/Time of Collection
    val tr: StringType?,    // Test Result
    val tc: StringType?,    // Testing Centre
    val co:	StringType?,    // Country of Test
    val `is`:StringType?,   // Certificate Issuer
    val ci:	StringType?     // Unique Certificate Identifier: UVCI
): FHIRLogical()

class Recovery(
    val tg: StringType?,	// disease or agent targeted
    val fr: DateType?,	    // ISO 8601 complete date of first positive NAA test result
    val df: DateType?,	    // ISO 8601 complete date: Certificate Valid From
    val du: DateType?,	    // ISO 8601 complete date: Certificate Valid Until
    val co:	StringType?,    // Country of Test
    val `is`: StringType?,  // Certificate Issuer
    val ci:	StringType?     // Unique Certificate Identifier: UVCI
): FHIRLogical()


object DecimalToDataTimeDeserializer: JsonDeserializer<DateTimeType>() {
    private fun parseDateType(date: Double?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(Date((date*1000).toLong()), TemporalPrecisionEnum.DAY)
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DateTimeType? {
        return parseDateType(p.decimalValue?.toDouble())
    }
}