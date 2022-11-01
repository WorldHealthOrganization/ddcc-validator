package org.who.ddccverifier.verify.divoc

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.verify.BaseModel

class W3CVC(
    @JsonProperty("@context")
    val context: List<StringType>,
    val type: List<StringType>,
    val issuer: StringType,
    val issuanceDate: DateTimeType,
    val nonTransferable: BooleanType?,
    val credentialSubject: CredentialSubject,
    val evidence: List<Evidence>,
    val proof: Proof?,
): BaseModel()

class CredentialSubject(
    val type: StringType?,
    val uhid: StringType?,
    val refId: StringType?,
    val name: StringType?,
    val gender: StringType?,
    val sex: StringType?,
    @JsonDeserialize(using = AgeToQuantityDeserializer::class)
    val age: Quantity?, //V1
    val dob: DateTimeType?, //V2
    val nationality: StringType?,
    val address: Address?,
): BaseModel()

class Proof(
    val type: StringType?,
    val created: StringType?,
    val verificationMethod: StringType?,
    val proofPurpose: StringType?,
    val jws: StringType?,
): BaseModel()

class Address(
    val streetAddress: StringType?,
    val streetAddress2: StringType?,
    val district: StringType?,
    val city: StringType?,
    val addressRegion: StringType?,
    val addressCountry: StringType?,
    @JsonDeserialize(using = DecimalToStringDeserializer::class)
    val postalCode: StringType?,
): BaseModel()

object DecimalToStringDeserializer: JsonDeserializer<StringType>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): StringType {
        return StringType(p.valueAsString)
    }
}

object AgeToQuantityDeserializer: JsonDeserializer<Quantity>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Quantity {
        return Quantity().apply {
            this.value = p.valueAsString.toBigDecimal()
            this.unit = "years"
        }
    }
}

class Evidence(
    val feedbackUrl: StringType?,
    val infoUrl: StringType?,
    val certificateId: StringType?,
    val type: List<StringType>?,
    val batch: StringType?,
    val vaccine: StringType?,
    val manufacturer: StringType?,
    val date: DateTimeType?,
    val effectiveStart: DateTimeType?,
    val effectiveUntil: DateTimeType?,
    val dose: PositiveIntType?,
    val totalDoses: PositiveIntType?,
    val verifier: Verifier?,
    val facility: Facility?,
    val icd11Code: StringType?,  //V2
    val prophylaxis: StringType?,  //V2
): BaseModel()

class Verifier(
    val name: StringType?,
): BaseModel()

class Facility(
    val name: StringType?,
    val address: Address?,
): BaseModel()