package org.who.ddccverifier.services.qrs.hcert

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.r4.model.*
import java.util.*

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class DCC2FHIR {
    val CWT_ISSUER = 1
    val CWT_SUBJECT = 2
    val CWT_AUDIENCE = 3
    val CWT_EXPIRATION = 4
    val CWT_NOT_BEFORE = 5
    val CWT_ISSUED_AT = 6
    val CWT_ID = 7
    val HCERT = -260
    val HCERT_V1 = 1
    val UY_STRING_PAYLOAD = 99


    private fun parseDateType(date: CBORObject?): DateType? {
        if (date == null || date.isUndefined) return null
        return if (date.isNumber) {
            DateType(Date(date.AsInt64()*1000))
        } else {
            DateType(date.AsString())
        }
    }

    private fun parseDateTimeType(date: CBORObject?): DateTimeType? {
        if (date == null || date.isUndefined) return null
        return if (date.isNumber) {
            DateTimeType(Date(date.AsInt64()*1000), TemporalPrecisionEnum.DAY)
        } else {
            DateTimeType(date.AsString())
        }
    }

    private fun parsePositiveIntType(positiveInt: CBORObject?): PositiveIntType? {
        if (positiveInt == null || positiveInt.isUndefined) return null
        return PositiveIntType().apply {
            value = positiveInt.AsInt32Value()
        }
    }

    private fun parseIdentifier(obj: CBORObject?): Identifier? {
        if (obj == null || obj.isUndefined) return null
        return Identifier().apply {
            value = obj.AsString()
        }
    }

    private fun parseIdentifierReference(obj: CBORObject?): Reference? {
        if (obj == null || obj.isUndefined) return null
        return Reference().apply {
            if (obj.type == CBORType.Map) {
                identifier = Identifier().apply {
                    value = obj["code"]?.AsString()
                    system = obj["system"]?.AsString()
                }
            } else {
                id = obj.AsString()
            }
        }
    }

    private fun parseCoding(obj: CBORObject?, st: String): Coding? {
        if (obj == null || obj.isUndefined) return null
        return Coding().apply {
            code = obj.AsString()
            system = st
        }
    }

    private fun parseCodableConcept(obj: CBORObject?, system: String): CodeableConcept? {
        if (obj == null || obj.isUndefined) return null
        return CodeableConcept(parseCoding(obj, system))
    }

    private fun parseOrganization(obj: CBORObject?): Organization? {
        if (obj == null || obj.isUndefined) return null
        return Organization().apply {
            identifier = listOf(Identifier().apply {
                value = obj.AsString()
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

    private fun parseHumanName(obj: CBORObject?): HumanName? {
        if (obj == null || obj.isUndefined) return null
        return HumanName().apply {
            use = HumanName.NameUse.OFFICIAL
            family = obj["fn"].AsString()
            addGiven(obj["gn"].AsString())
        }
    }


    private fun parseTransliteratedHumanName(obj: CBORObject?): HumanName? {
        if (obj == null || obj.isUndefined) return null
        return HumanName().apply {
            use = HumanName.NameUse.OFFICIAL
            family = obj["fnt"].AsString()
            addGiven(obj["gnt"].AsString())
        }
    }

    fun run(CWT: CBORObject): Composition {
        val iss = CWT[CWT_ISSUER]
        val sub = CWT[CWT_SUBJECT]
        val aud = CWT[CWT_AUDIENCE]
        val exp = CWT[CWT_EXPIRATION]
        val nbf = CWT[CWT_NOT_BEFORE]
        val iat = CWT[CWT_ISSUED_AT]
        val cert = CWT[HCERT][HCERT_V1]

        val myPatient = Patient().apply{
            name = listOfNotNull(
                parseHumanName(cert["nam"]),
                parseTransliteratedHumanName(cert["nam"])
            )
            birthDateElement = parseDateType(cert["dob"])
        }

        val immunizations = mutableListOf<Reference>()
        val authors = mutableListOf<Reference>()

        if (!cert["v"].isUndefined) {
            for (i in 0 until cert["v"].size()) {
                val vax = cert["v"][i]
                val myAuthority = parseOrganization(vax["is"])
                val myImmunization = Immunization().apply {
                    identifier = listOfNotNull(parseIdentifier(vax["ci"]))
                    patient = Reference(myPatient)
                    vaccineCode = parseCodableConcept(vax["vp"], "http://snomed.info/sct")
                    occurrence = parseDateTimeType(vax["dt"])
                    protocolApplied =
                        listOfNotNull(Immunization.ImmunizationProtocolAppliedComponent().apply {
                            targetDisease = listOfNotNull(parseCodableConcept(vax["tg"], "http://snomed.info/sct"))
                            doseNumber = parsePositiveIntType(vax["dn"])
                            seriesDoses = parsePositiveIntType(vax["sd"])
                            authority = Reference(myAuthority)
                        })
                    manufacturer = parseIdentifierReference(vax["ma"])
                    extension = listOfNotNull(
                        parseExtension(parseCoding(vax["mp"], ""),"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand"),
                        parseExtension(parseCoding(vax["ma"], ""),"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization"),
                        parseExtension(parseCoding(vax["co"], "urn:iso:std:iso:3166"),"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination"),
                    )
                }

                authors.add(Reference(myAuthority))
                immunizations.add(Reference(myImmunization))
            }
        }

        val myComposition = Composition().apply {
            type = CodeableConcept(Coding("http://loinc.org", "82593-5", "Immunization summary report"))
            category = listOf(CodeableConcept(Coding().apply {
                code = "ddcc-vs"
            }))
            subject = Reference(myPatient)
            title = "International Certificate of Vaccination or Prophylaxis"
            event = listOf(Composition.CompositionEventComponent().apply {
                period = Period().apply {
                    startElement = parseDateTimeType(nbf) ?: parseDateTimeType(iat)
                    endElement = parseDateTimeType(exp)
                }
            })
            author = authors
            section = listOf(Composition.SectionComponent().apply {
                code = CodeableConcept(Coding("http://loinc.org", "11369-6", "History of Immunization Narrative"))
                author = authors
                entry = immunizations
            })
        }

        // Is this really necessary? Why aren't these objects part of contained to start with?
        myComposition.addContained(myPatient)

        for (imm in immunizations) {
            myComposition.addContained(imm.resource as Resource)
        }

        return myComposition
    }
}