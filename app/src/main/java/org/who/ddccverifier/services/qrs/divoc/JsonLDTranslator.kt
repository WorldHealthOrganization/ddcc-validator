package org.who.ddccverifier.services.qrs.divoc

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.r4.model.*
import java.util.*

/**
 * Translates the JSONLD content into FHIR
 */
class JsonLDTranslator {

    private fun parseAge(issuanceDate: Date, age: String?): DateType? {
        if (age == null) return null
        val dob = Calendar.getInstance()
            dob.time = issuanceDate
            dob.add(Calendar.YEAR, -Integer.parseInt(age))
        return DateType(dob.time)
    }

    private fun parseDateType(date: Date?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(date, TemporalPrecisionEnum.DAY)
    }

    private fun parseDateTimeType(date: Date?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(date)
    }

    private fun parseGender(gender: String?): Enumerations.AdministrativeGender? {
        if (gender == null) return null
        return Enumerations.AdministrativeGender.fromCode(gender.lowercase())
    }

    private fun parseCoding(cd: String?, st: String): Coding? {
        if (cd == null || cd.isEmpty()) return null
        return Coding().apply {
            code = cd
            system = st
        }
    }

    private fun parsePerformer(verifier: String?) : Immunization.ImmunizationPerformerComponent? {
        if (verifier == null || verifier.isEmpty()) return null
        return Immunization.ImmunizationPerformerComponent().apply {
            actor = Reference(Practitioner().apply {
                name = listOf(HumanName().apply {
                    text = verifier
                })
            })
        }
    }

    private fun parseExtension(value: IBaseDatatype?, url: String): Extension? {
        if (value == null) return null
        return Extension().apply {
            setUrl(url)
            setValue(value)
        }
    }

    private fun parsePositiveIntType(value: Int?): Type? {
        if (value == null) return null
        return PositiveIntType(value)
    }

    private fun parseVaccine(vaccine: String?): CodeableConcept? {
        if (vaccine == null || vaccine.isEmpty()) return null
        return CodeableConcept().apply {
            coding = listOf(Coding().apply {
                display = vaccine
            })
        }
    }

    fun toFhir(vc: DivocVerifier.W3CVC): Composition {
        val myPatient = Patient().apply{
            id = vc.credentialSubject.refId
            name = listOfNotNull(HumanName().apply {
                    text = vc.credentialSubject.name
            })
            identifier = listOfNotNull(Identifier().apply {
                this.value = vc.credentialSubject.id
            })
            birthDateElement = parseAge(vc.issuanceDate, vc.credentialSubject.age)
            gender = parseGender(vc.credentialSubject.gender)
            addAddress().apply {
                addLine(vc.credentialSubject.address?.streetAddress)
                addLine(vc.credentialSubject.address?.streetAddress2)
                city = vc.credentialSubject.address?.city
                district = vc.credentialSubject.address?.district
                postalCode = vc.credentialSubject.address?.postalCode
                state = vc.credentialSubject.address?.addressRegion
                country = vc.credentialSubject.address?.addressCountry
            }
        }

        val refImmunizations = mutableListOf<Reference>()
        val immunizations = mutableListOf<Immunization>()
        val phas = mutableListOf<Reference>()

        for (ev in vc.evidence) {
            val pha = Organization().apply {
                this.name = ev.facility?.name
            }

            phas.add(Reference(pha))

            val myImmunization = Immunization().apply{
                identifier = listOf(Identifier().apply{
                    value = ev.certificateId
                })
                patient = Reference(myPatient)
                vaccineCode = parseVaccine(ev.vaccine)
                occurrence = parseDateTimeType(ev.date)
                lotNumber = ev.batch
                protocolApplied = listOfNotNull(Immunization.ImmunizationProtocolAppliedComponent().apply {
                    targetDisease = listOfNotNull(CodeableConcept().apply {
                        coding = listOf(Coding().apply {
                            code = "840539006"
                            system = "http://snomed.info/sct"
                        })
                    })
                    doseNumber = parsePositiveIntType(ev.dose)
                    seriesDoses = parsePositiveIntType(ev.totalDoses)
                    authority = Reference(pha)
                })
                location = Reference(
                    Location().apply {
                        name = ev.facility?.name
                        address = Address().apply {
                            addLine(ev.facility?.address?.streetAddress)
                            addLine(ev.facility?.address?.streetAddress2)
                            city = ev.facility?.address?.city
                            district = ev.facility?.address?.district
                            postalCode = ev.facility?.address?.postalCode
                            state = ev.facility?.address?.addressRegion
                            country = ev.facility?.address?.addressCountry
                        }
                    }
                )
                performer = listOfNotNull(parsePerformer(ev.verifier?.name))
                manufacturer = Reference(
                    Organization().apply {
                        name = ev.manufacturer
                    }
                )
                extension = listOfNotNull(
                    //parseExtension(parseCoding(ev.vaccine), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand"),
                    //parseExtension(parseCoding(DDCC["ma_holder"]), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization"),
                    parseExtension(parseCoding(ev.facility?.address?.addressCountry, "urn:iso:std:iso:3166"), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination"),
                    parseExtension(parseDateTimeType(ev.effectiveUntil), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineValidFrom"),
                )
            }

            immunizations.add(myImmunization)
            refImmunizations.add(Reference(myImmunization))
        }

        val myComposition = Composition().apply {
            id = vc.credentialSubject.refId
            type = CodeableConcept(Coding("http://loinc.org", "82593-5", "Immunization summary report"))
            category = listOf(CodeableConcept(Coding().apply {
                code = "ddcc-vs"
            }))
            subject = Reference(myPatient)
            title = "International Certificate of Vaccination or Prophylaxis"
            event = listOf(Composition.CompositionEventComponent().apply {
                period = Period().apply {
                    startElement = parseDateType(vc.issuanceDate)
                }
            })
            author = phas
            section = listOf(Composition.SectionComponent().apply {
                code = CodeableConcept(Coding("http://loinc.org", "11369-6", "History of Immunization Narrative"))
                author = phas
                entry = refImmunizations
            })
        }

        // Is this really necessary? Why aren't these objects part of contained to start with?
        myComposition.addContained(myPatient)

        for (imm in immunizations) {
            myComposition.addContained(imm as Resource)
        }

        return myComposition
    }
}