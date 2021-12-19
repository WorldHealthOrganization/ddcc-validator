package org.who.ddccverifier.services.qrs.shc

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import org.hl7.fhir.r4.model.*
import java.util.*

/**
 * Finds the right translator for the JWT content using SHC
 */
class JWTTranslator {

    fun getPatient(payload: SHCVerifier.JWTPayload): Patient {
        return payload.vc?.credentialSubject?.fhirBundle?.entry
            ?.filter { it -> it.resource.fhirType() == "Patient" }
            ?.first()?.resource as Patient
    }

    fun getImmunizations(payload: SHCVerifier.JWTPayload): List<Immunization> {
        return payload.vc?.credentialSubject?.fhirBundle?.entry
            ?.filter { it -> it.resource.fhirType() == "Immunization" }
            ?.map {it -> it.resource} as List<Immunization>
    }

    private fun parseDateType(date: Double?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(Date((date*1000).toLong()), TemporalPrecisionEnum.DAY)
    }

    fun toFhir(payload: SHCVerifier.JWTPayload): Composition {
        val myPatient = getPatient(payload)
        val myImmunizations = getImmunizations(payload)

        val organization = Organization().apply {
            identifier = listOf(Identifier().apply {
                value = payload.iss
            })
        }

        val immunizations = mutableListOf<Reference>()
        for (imm in myImmunizations) {
            immunizations.add(Reference(imm))
        }

        val myComposition = Composition().apply {
            id = payload.jti
            type = CodeableConcept(Coding("http://loinc.org", "82593-5", "Immunization summary report"))
            category = listOf(CodeableConcept(Coding().apply {
                code = "ddcc-vs"
            }))
            subject = Reference(myPatient)
            title = "International Certificate of Vaccination or Prophylaxis"
            event = listOf(Composition.CompositionEventComponent().apply {
                period = Period().apply {
                    startElement = parseDateType(payload.nbf)
                    endElement = parseDateType(payload.exp)
                }
            })
            author = listOf(Reference(organization))
            section = listOf(Composition.SectionComponent().apply {
                code = CodeableConcept(Coding("http://loinc.org", "11369-6", "History of Immunization Narrative"))
                author = listOf(Reference(organization))
                entry = immunizations
            })
        }

        // Is this really necessary? Why aren't these objects part of contained to start with?
        myComposition.addContained(myPatient)

        for (imm in myImmunizations) {
            myComposition.addContained(imm)
        }

        return myComposition
    }
}