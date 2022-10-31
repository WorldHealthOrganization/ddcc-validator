package org.who.ddccverifier.verify.divoc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.utils.FHIRLogical

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
): FHIRLogical()

class CredentialSubject(
    val type: StringType?,
    val uhid: StringType?,
    val refId: StringType?,
    val name: StringType?,
    val gender: StringType?,
    val sex: StringType?,
    val age: StringType?, //V1
    val dob: DateTimeType?, //V2
    val nationality: StringType?,
    val address: Address?,
): FHIRLogical()

class Proof(
    val type: StringType?,
    val created: StringType?,
    val verificationMethod: StringType?,
    val proofPurpose: StringType?,
    val jws: StringType?,
): FHIRLogical()

class Address(
    val streetAddress: StringType?,
    val streetAddress2: StringType?,
    val district: StringType?,
    val city: StringType?,
    val addressRegion: StringType?,
    val addressCountry: StringType?,
    @JsonDeserialize(using = DecimalToStringDeserializer::class)
    val postalCode: StringType?,
): FHIRLogical()

object DecimalToStringDeserializer: JsonDeserializer<StringType>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): StringType {
        return StringType(p.valueAsString)
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
): FHIRLogical()

class Verifier(
    val name: StringType?,
): FHIRLogical()

class Facility(
    val name: StringType?,
    val address: Address?,
): FHIRLogical()