package org.who.ddccverifier.verify.shc

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.StringType
import org.who.ddccverifier.verify.BaseModel
import java.util.*

class JWTPayload(
    val iss: StringType?,
    val sub: StringType?,
    val aud: StringType?,
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val exp: DateTimeType?, // it should be int, but some idiots use floating point
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val nbf: DateTimeType?, // it should be int, but some idiots use floating point
    @JsonDeserialize(using = DecimalToDataTimeDeserializer::class)
    val iat: DateTimeType?, // it should be int, but some idiots use floating point
    val jti: StringType?,
    val vc: VC?,
): BaseModel()

class VC(
    val type: List<StringType>?,
    val credentialSubject: CredentialSubject?,
): BaseModel()

class CredentialSubject(
    val fhirVersion: StringType?,
    @JsonDeserialize(using = FHIRDeserializer::class)
    @JsonSerialize(using = FHIRSeserializer::class)
    val fhirBundle: Bundle?,
): BaseModel()

object DecimalToDataTimeDeserializer: JsonDeserializer<DateTimeType>() {
    private fun parseDateType(date: Double?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(Date((date*1000).toLong()), TemporalPrecisionEnum.DAY)
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DateTimeType? {
        return parseDateType(p.decimalValue?.toDouble())
    }
}

object FHIRDeserializer : JsonDeserializer<Bundle>() {
    val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Bundle {
        val node = p.readValueAsTree<JsonNode>()
        return fhirContext.newJsonParser().parseResource(node.toPrettyString()) as Bundle
    }
}

object FHIRSeserializer : JsonSerializer<Bundle>() {
    val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)

    override fun serialize(value: Bundle?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        if (value != null) {
            val str = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(value)
            gen?.writeRaw(":" + str)
        }
    }
}
