package org.who.ddccverifier.verify.hcert.dcc.logical

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.verify.BaseModel
import kotlin.reflect.full.declaredMemberProperties

class WHOLogicalModel (
    val meta: Meta?,

    val name: StringType?,
    val birthDate: DateType?,
    val sex: Coding?,
    val identifier: StringType?,

    // test
    val test: TestResult?,

    // certificate
    val hcid: StringType?,
    val valid_from: DateTimeType?,
    val valid_until: DateTimeType?,

    // Vaccine fields are in the root
    val date: DateTimeType?,
    val due_date: DateTimeType?,
    val vaccine_valid: DateType?,
    val hw: StringType?,
    val disease: Coding?,
    val centre: StringType?,
    val vaccine: Coding?,
    val lot: StringType?,
    val dose: PositiveIntType?,
    val total_doses: PositiveIntType?,

    val brand: Coding?,
    val ma_holder: Coding?,
    @JsonDeserialize(using = CodingOrReferenceDeserializer::class)
    val manufacturer: Base?,
    val pha: StringType?,
    val country: Coding?
): BaseModel()

class TestResult (
    val pathogen: Coding?,
    val type: Coding?,
    val brand: Coding?,
    val manufacturer: Coding?,
    val origin: Coding?,
    val date: DateTimeType?,
    val result: Coding?,
    val centre: StringType?,
    val country: Coding?
): BaseModel()

class Meta (
    val notarisedOn: DateTimeType?,
    val reference: StringType?,
    val url: StringType?,
    val passportNumber: StringType?
): org.hl7.fhir.r4.model.Meta() {
    private val propertiesByHash = this::class.declaredMemberProperties.associateBy { it.name.hashCode() }

    override fun getProperty(hash: Int, name: String?, checkValid: Boolean): Array<Base?> {
        return propertiesByHash[hash]?.let {
            val prop = it.getter.call(this)
            if (prop == null) {
                emptyArray()
            } else if (prop is Base) {
                arrayOf(prop)
            } else if (prop is Collection<*>) {
                if (prop.isEmpty()) {
                    emptyArray()
                } else {
                    (prop as Collection<Base?>).toTypedArray()
                }
            } else {
                emptyArray()
            }
        } ?: super.getProperty(hash, name, checkValid)
    }
}

object CodingOrReferenceDeserializer: JsonDeserializer<Base>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Base? {
        val token: TreeNode = p.readValueAsTree()

        return if (token.isValueNode) {
            Reference().apply {
                id = token.toString()
            }
        } else {
            return jacksonObjectMapper().readValue<Coding>(token.toString())
        }
    }
}