package org.who.ddccverifier.services.qrs.icao

import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.r4.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Translates the JSONLD content into FHIR
 */
class IJsonTranslator {

    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    private fun parseAge(issuanceDate: String?, age: String?): DateType? {
        if (age == null) return null
        val dob = Calendar.getInstance()
            dob.time = isoFormatter.parse(issuanceDate)
            dob.add(Calendar.YEAR, -Integer.parseInt(age))
        return DateType(dob.get(Calendar.YEAR).toString())
    }

    private fun parseDoB(date: String?): DateType? {
        if (date == null) return null
        return DateType(date)
    }

    private fun parseDateType(date: String?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(date)
    }

    private fun parseDateTimeType(date: String?): DateTimeType? {
        if (date == null) return null
        return DateTimeType(date)
    }

    private fun parseGender(gender: String?): Enumerations.AdministrativeGender? {
        if (gender == null) return null
        return when (gender) {
            "M" -> Enumerations.AdministrativeGender.MALE
            "F" -> Enumerations.AdministrativeGender.FEMALE
            else -> Enumerations.AdministrativeGender.UNKNOWN
        }
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

    private fun parseVaccine(vaccineBrand: String?): CodeableConcept? {
        if (vaccineBrand == null) return null
        return CodeableConcept().apply {
            coding = listOf(Coding().apply {
                display = vaccineBrand
            })
        }
    }

    private fun parseVaccineCode(icd11Code: String?): CodeableConcept? {
        if (icd11Code == null) return null
        return CodeableConcept().apply {
            coding = listOf(Coding().apply {
                code = icd11Code
                system = "http://hl7.org/fhir/sid/icd-11"
            })
        }
    }

    fun parseIdentifier(value: String?): Identifier? {
        if (value == null || value.isBlank()) return null
        return Identifier().apply { this.value = value}
    }

    val DOC_TYPES = mapOf(
        "P" to "PPN",  // P – Passport (Doc 9303-4)
        "A" to "DL",   // A – ID Card (Doc 9303-5)
        "C" to "DL",   // C – ID Card (Doc 9303-5)
        "I" to "DL",   // I – ID Card Doc 9303-5)
        "AC" to "EN",  // AC - Crew Member Certificate (Doc 9303-5)
        "V" to "ACSN", // V – Visa (Doc 9303-7)
        "D" to "DL",   // D – Driving License (ISO18013-1)
    )

    fun parseIdentifier(value: String?, type: String?): Identifier? {
        if (value == null || value.isBlank()) return null
        return Identifier().apply {
            this.value = DOC_TYPES[value]
            this.system = "http://hl7.org/fhir/ValueSet/identifier-type"
        }
    }

    fun toFhir(vc: IcaoVerifier.IJson): Composition {
        val myPatient = Patient().apply{
            identifier = listOfNotNull(
                parseIdentifier(vc.data.msg.pid?.i),
                parseIdentifier(vc.data.msg.pid?.ai),
                parseIdentifier(vc.data.msg.pid?.dn, vc.data.msg.pid?.dt)
            )
            name = listOfNotNull(HumanName().apply {
                text = vc.data.msg.pid?.n
            })
            gender = parseGender(vc.data.msg.pid?.sex)
            birthDateElement = parseDoB(vc.data.msg.pid?.dob)
        }

        val refImmunizations = mutableListOf<Reference>()
        val immunizations = mutableListOf<Immunization>()
        val phas = mutableListOf<Reference>()

        for (vaccinationEvent in vc.data.msg.ve.orEmpty()) {
            for (vaccinationDetails in vaccinationEvent.vd.orEmpty()) {
                val pha = Organization().apply {
                    this.name = vaccinationDetails.adm
                }

                phas.add(Reference(pha))

                val myImmunization = Immunization().apply {
                    patient = Reference(myPatient)
                    vaccineCode = parseVaccineCode(vaccinationEvent.des)
                    occurrence = parseDateTimeType(vaccinationDetails.dvc)
                    lotNumber = vaccinationDetails.lot
                    protocolApplied =
                        listOfNotNull(Immunization.ImmunizationProtocolAppliedComponent().apply {
                            targetDisease = listOfNotNull(CodeableConcept().apply {
                                coding = listOf(Coding().apply {
                                    code = vaccinationEvent.dis
                                    system = "http://hl7.org/fhir/sid/icd-11"
                                })
                            })
                            doseNumber = parsePositiveIntType(vaccinationDetails.seq)
                            authority = Reference(pha)
                        })
                    manufacturer = Reference(
                        Organization().apply {
                            name = vaccinationDetails.adm
                        }
                    )
                    extension = listOfNotNull(
                        parseExtension(parseVaccine(vaccinationEvent.nam),
                            "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand"),
                        parseExtension(parseCoding(vaccinationDetails.ctr,"urn:iso:std:iso:3166"),
                            "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination")
                    )
                }

                immunizations.add(myImmunization)
                refImmunizations.add(Reference(myImmunization))
            }
        }

        val myComposition = Composition().apply {
            id = vc.data.msg.ucti ?: vc.data.msg.uvci
            type = CodeableConcept(Coding("http://loinc.org", "82593-5", "Immunization summary report"))
            category = listOf(CodeableConcept(Coding().apply {
                code = "ddcc-vs"
            }))
            subject = Reference(myPatient)
            title = "International Certificate of Vaccination or Prophylaxis"
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