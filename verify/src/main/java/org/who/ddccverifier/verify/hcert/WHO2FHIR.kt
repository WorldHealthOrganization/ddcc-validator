package org.who.ddccverifier.verify.hcert

import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import org.hl7.fhir.instance.model.api.IBaseDatatype
import org.hl7.fhir.r4.model.*

/**
 * Translates an EU DCC QR CBOR payload into FHIR Objects
 */
class WHO2FHIR {
    private fun parseDateType(date: CBORObject?): DateType? {
        if (date == null || date.isUndefined) return null
        return DateType(date.AsString())
    }

    private fun parseDateTimeType(date: CBORObject?): DateTimeType? {
        if (date == null || date.isUndefined) return null
        return DateTimeType(date.AsString())
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

    private fun parsePerformer(obj: CBORObject?) : Immunization.ImmunizationPerformerComponent? {
        if (obj == null || obj.isUndefined) return null
        return Immunization.ImmunizationPerformerComponent().apply {
            actor = Reference(Practitioner().apply {
                identifier = listOf(Identifier().apply {
                    value = obj.AsString()
                })
            })
        }
    }

    private fun parseLocation(obj: CBORObject?): Reference? {
        if (obj == null || obj.isUndefined) return null
        return Reference().apply {
            display = obj.AsString()
        }
    }

    private fun parseCoding(obj: CBORObject?): Coding? {
        if (obj == null || obj.isUndefined) return null
        return Coding().apply {
            code = obj["code"]?.AsString()
            system = obj["system"]?.AsString()
            display = obj["display"]?.AsString()
        }
    }

    private fun parseCodableConcept(obj: CBORObject?): CodeableConcept? {
        if (obj == null || obj.isUndefined) return null
        return CodeableConcept(parseCoding(obj))
    }

    private fun parseCodableConcept(obj1: CBORObject?, obj2: CBORObject?): CodeableConcept? {
        if ((obj1 == null || obj1.isUndefined) && (obj2 == null || obj2.isUndefined)) return null
        return CodeableConcept().apply {
            if (obj1 != null && !obj1.isUndefined) addCoding(parseCoding(obj1))
            if (obj2 != null && !obj2.isUndefined) addCoding(parseCoding(obj2))
        }
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

    private fun parseGender(obj: CBORObject?): Enumerations.AdministrativeGender? {
        if (obj == null || obj.isUndefined) return null
        return Enumerations.AdministrativeGender.fromCode(obj["code"].AsString())
    }

    private fun parseHumanName(obj: CBORObject?): HumanName? {
        if (obj == null || obj.isUndefined) return null
        return HumanName().apply {
            text = obj.AsString()
        }
    }

    private fun parseTestResults(test: CBORObject?): Observation? {
        if (test == null || test.isUndefined) return null

        return Observation().apply {
            status = Observation.ObservationStatus.FINAL
            category = listOf(CodeableConcept().apply {
                coding = listOf(Coding().apply {
                    code = "laboratory"
                    system = "http://terminology.hl7.org/CodeSystem/observation-category"
                })
            })

            effective = parseDateTimeType(test["date"])
            code = parseCodableConcept(test["type"], test["pathogen"])
            value = parseCodableConcept(test["result"])

            encounter = Reference(Encounter().apply {
                status = Encounter.EncounterStatus.FINISHED
                serviceProvider = Reference(parseOrganization(test["centre"]))
            })
            extension = listOfNotNull(
                parseExtension(parseCoding(test["country"]), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination"),
            )
        }
    }

    private fun parseImmunization(DDCC: CBORObject): Immunization? {
        if (DDCC["vaccine"] == null || DDCC["vaccine"].isUndefined) return null

        return Immunization().apply{
            vaccineCode = parseCodableConcept(DDCC["vaccine"])
            occurrence = parseDateTimeType(DDCC["date"])
            lotNumber = DDCC["lot"]?.AsString()
            protocolApplied = listOfNotNull(Immunization.ImmunizationProtocolAppliedComponent().apply {
                targetDisease = listOfNotNull(parseCodableConcept(DDCC["disease"]))
                doseNumber = parsePositiveIntType(DDCC["dose"])
                seriesDoses = parsePositiveIntType(DDCC["total_doses"])
                authority = Reference(parseOrganization(DDCC["pha"]))
            })
            location = parseLocation(DDCC["centre"])
            performer = listOfNotNull(parsePerformer(DDCC["hw"]))
            manufacturer = parseIdentifierReference(DDCC["manufacturer"])
            extension = listOfNotNull(
                parseExtension(parseCoding(DDCC["brand"]), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand"),
                parseExtension(parseCoding(DDCC["ma_holder"]), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization"),
                parseExtension(parseCoding(DDCC["country"]), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination"),
                parseExtension(parseDateTimeType(DDCC["vaccine_valid"]), "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineValidFrom"),
            )
        }
    }

    private fun createRecommendationBasedOn(due_date: CBORObject?, immunization: Immunization?): ImmunizationRecommendation? {
        if (due_date == null || due_date.isUndefined) return null
        if (immunization == null) return null
        return ImmunizationRecommendation().apply {
            patient = immunization.patient
            recommendation = listOf(ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent().apply {
                vaccineCode = listOfNotNull(immunization.vaccineCode)
                targetDisease = immunization.protocolAppliedFirstRep.targetDiseaseFirstRep
                doseNumber = PositiveIntType().setValue(immunization.protocolAppliedFirstRep.doseNumberPositiveIntType.value +1)
                seriesDoses = immunization.protocolAppliedFirstRep.seriesDoses
                forecastStatus = CodeableConcept(Coding("http://terminology.hl7.org/2.1.0/CodeSystem-immunization-recommendation-status.html", "due", ""))
                dateCriterion = listOf(ImmunizationRecommendation.ImmunizationRecommendationRecommendationDateCriterionComponent().apply {
                    code = CodeableConcept(Coding("http://loinc.org", "30980-7", "Date vaccine due"))
                    valueElement = parseDateTimeType(due_date)
                })
            })
        }
    }

    fun run(DDCC: CBORObject): Bundle {
        val myPatient = Patient().apply{
            name = listOfNotNull(parseHumanName(DDCC["name"]))
            identifier = listOfNotNull(parseIdentifier(DDCC["identifier"]))
            birthDateElement = parseDateType(DDCC["birthDate"])
            gender = parseGender(DDCC["sex"])
        }

        val myImmunization = parseImmunization(DDCC)?.apply {
            patient = Reference().apply {
                identifier = parseIdentifier(DDCC["identifier"])
            }
        }

        val myTestResult = parseTestResults(DDCC["test"])?.apply{
            subject = Reference().apply {
                identifier = parseIdentifier(DDCC["identifier"])
            }
        }

        val myRecommendation = createRecommendationBasedOn(DDCC["due_date"], myImmunization)

        val myComposition = Composition().apply {
            id = DDCC["hcid"]?.AsString()
            type = CodeableConcept(Coding("http://loinc.org", "82593-5", "Immunization summary report"))
            category = listOf(CodeableConcept(Coding().apply {
                code = "ddcc-vs"
            }))
            subject = Reference(myPatient)
            title = "International Certificate of Vaccination or Prophylaxis"
            event = listOf(Composition.CompositionEventComponent().apply {
                period = Period().apply {
                    startElement = parseDateTimeType(DDCC["valid_from"])
                    endElement = parseDateTimeType(DDCC["valid_until"])
                }
            })
            author = listOfNotNull(
                myImmunization?.protocolAppliedFirstRep?.authority,
                (myTestResult?.encounter?.resource as? Encounter)?.serviceProvider
            )
            section = listOfNotNull(
                myImmunization?.let { Composition.SectionComponent().apply {
                    code = CodeableConcept(Coding("http://loinc.org", "11369-6", "History of Immunization Narrative"))
                    author = listOfNotNull(myImmunization.protocolAppliedFirstRep.authority)
                    focus = Reference(myImmunization)
                    entry = listOfNotNull(
                        Reference(myImmunization),
                        myRecommendation?.let { Reference(myRecommendation) }
                    )
                }},
                myTestResult?.let { Composition.SectionComponent().apply {
                    code = CodeableConcept(Coding("http://loinc.org", "30954-2", "Results (Diagnostic findings)"))
                    author = listOfNotNull((myTestResult.encounter.resource as Encounter).serviceProvider)
                    focus = Reference(myTestResult)
                    entry = listOfNotNull(
                        Reference(myTestResult),
                        myRecommendation?.let { Reference(myRecommendation) }
                    )
                }}
            )
        }

        val b = Bundle()
        b.type = Bundle.BundleType.TRANSACTION
        b.addEntry().resource = myComposition
        b.addEntry().resource = myPatient
        myImmunization?.let { b.addEntry().resource = it }
        myTestResult?.let { b.addEntry().resource = it }
        myTestResult?.let { b.addEntry().resource = it.encounter.resource as Encounter }
        myRecommendation?.let { b.addEntry().resource = it }

        return b
    }
}