package org.who.ddccverifier.services

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.views.ResultFragment
import java.text.SimpleDateFormat
import java.util.*

/**
 * Converts a FHIR Object into a Result Card for the Screne.
 */
class DDCCFormatter {
    private val fmt = SimpleDateFormat("MMM d, yyyy")
    private val fmtYear = SimpleDateFormat("yyyy")
    private val fmtYearMonth = SimpleDateFormat("MMM, yyyy")
    private val fmtComplete = SimpleDateFormat("MMM d, h:mma")

    private val DISEASES = mapOf(
        "840539006" to "COVID-19"
    )

    private val ICD11_TARGETS = mapOf(
        "RA01" to "COVID-19",
        "RA01.0" to "COVID-19",
    )

    private val ICD11_DIAGNOSIS = mapOf(
        "RA01" to "COVID-19",
        "RA01.0" to "COVID-19, virus identified",
        "RA01.1" to "COVID-19, virus not identified",
        "RA01.0/CA40.1" to "COVID-19 with pneumonia, SARS-CoV-2 identified",
        "RA01.1/CA40.1" to "COVID-19 with pneumonia, SARS-CoV-2 not identified"
    )

    private val ICD11 = mapOf(
        "RA01" to "COVID-19",
        "RA01.0" to "COVID-19, virus identified",
        "RA01.1" to "COVID-19, virus not identified",
        "RA01.0/CA40.1" to "COVID-19 with pneumonia, SARS-CoV-2 identified",
        "RA01.1/CA40.1" to "COVID-19 with pneumonia, SARS-CoV-2 not identified"
    )

    //vaccinationEvent.dis, "http://hl7.org/fhir/sid/icd-11"

    private val VACCINE_PROPH = mapOf(
        "1119349007" to "SARS-CoV-2 mRNA Vaccine" ,
        "1119305005" to "SARS-CoV-2 Antigen Vaccine",
        "J07BX03" to "COVID-19 Vaccine"
    )
    private val CVX = mapOf(
        "2"	to "ORIMUNE",
        "3"	to "M-M-R II",
        "5"	to "ATTENUVAX",
        "6"	to "MERUVAX II",
        "7"	to "MUMPSVAX",
        "8"	to "ENGERIX B-PEDS; RECOMBIVAX-PEDS",
        "9"	to "TDVAX; Td, adsorbed",
        "10"	to "IPOL",
        "18"	to "Imovax; RabAvert",
        "19"	to "MYCOBAX; TICE BCG",
        "20"	to "ACEL-IMUNE; CERTIVA; INFANRIX; TRIPEDIA",
        "21"	to "VARIVAX",
        "22"	to "TETRAMUNE",
        "24"	to "BIOTHRAX",
        "25"	to "VIVOTIF BERNA; Vivotif",
        "28"	to "DT(GENERIC)",
        "32"	to "MENOMUNE",
        "33"	to "PNEUMOVAX 23",
        "35"	to "TETANUS TOXOID (GENERIC)",
        "37"	to "YF-VAX",
        "38"	to "BIAVAX II",
        "39"	to "JE-VAX",
        "40"	to "IMOVAX ID",
        "43"	to "ENGERIX-B-ADULT; RECOMBIVAX-ADULT",
        "44"	to "RECOMBIVAX-DIALYSIS",
        "46"	to "PROHIBIT",
        "47"	to "HIBTITER",
        "48"	to "ACTHIB; HIBERIX; OMNIHIB",
        "49"	to "PEDVAXHIB",
        "50"	to "TRIHIBIT",
        "51"	to "COMVAX",
        "52"	to "HAVRIX-ADULT; VAQTA-ADULT",
        "53"	to "TYPHOID-AKD",
        "56"	to "DENGVAXIA",
        "62"	to "GARDASIL",
        "75"	to "ACAM2000; DRYVAX",
        "83"	to "HAVRIX-PEDS; VAQTA-PEDS",
        "94"	to "PROQUAD",
        "100"	to "PREVNAR 7",
        "101"	to "TYPHIM VI",
        "104"	to "TWINRIX",
        "106"	to "DAPTACEL",
        "110"	to "PEDIARIX",
        "111"	to "FLUMIST",
        "113"	to "DECAVAC; Tenivac",
        "114"	to "MENACTRA",
        "115"	to "ADACEL; BOOSTRIX",
        "116"	to "ROTATEQ",
        "118"	to "CERVARIX",
        "119"	to "ROTARIX",
        "120"	to "PENTACEL",
        "121"	to "ZOSTAVAX",
        "125"	to "Novel Influenza-H1N1-09, nasal",
        "126"	to "Novel influenza-H1N1-09, preservative-free",
        "127"	to "Novel influenza-H1N1-09",
        "130"	to "KINRIX; Quadracel",
        "133"	to "PREVNAR 13",
        "134"	to "IXIARO; Ixiaro",
        "135"	to "FLUZONE-HIGH DOSE",
        "136"	to "MENVEO; Menveo",
        "140"	to "AGRIFLU; Afluria, preservative free; FLUARIX; FLUVIRIN-PRESERVATIVE FREE; FLUZONE-PRESERVATIVE FREE; Flulaval, preservative free; Fluvirin preservative free",
        "141"	to "AFLURIA; Afluria; FLULAVAL; FLUVIRIN; FLUZONE; Fluvirin",
        "143"	to "Adenovirus types 4 and 7",
        "144"	to "Fluzone, intradermal",
        "146"	to "VAXELIS",
        "148"	to "MENHIBRIX",
        "149"	to "Flumist quadrivalent",
        "150"	to "Afluria quadrivalent preservative free; Fluarix, quadrivalent, preservative free; Flulaval, quadrivalent, preservative free; Fluzone, quadrivalent, preservative free",
        "153"	to "Flucelvax",
        "155"	to "Flublok",
        "158"	to "Afluria, quadrivalent; Flulaval quadrivalent; Fluzone, Quadrivalent",
        "160"	to "Influenza A (H5N1) -2013; Influenza A monovalent (H5N1), ADJUVANTED-2013",
        "161"	to "Afluria quadrivalent, preservative free, pediatric; Fluzone Quadrivalent, pediatric",
        "162"	to "Trumenba",
        "163"	to "Bexsero",
        "165"	to "Gardasil 9",
        "166"	to "Fluzone Quad Intradermal",
        "168"	to "Fluad",
        "171"	to "flucelvax, quadrivalent, preservative free",
        "174"	to "VAXCHORA",
        "175"	to "IMOVAX",
        "176"	to "RABAVERT",
        "183"	to "Stamaril",
        "185"	to "Flublok quadrivalent",
        "186"	to "Flucelvax, quadrivalent, with preservative",
        "187"	to "SHINGRIX",
        "189"	to "HEPLISAV-B",
        "197"	to "FLUZONE High-Dose Quadrivalent",
        "200"	to "FLUZONE Quadrivalent Southern Hemisphere, Pediatric",
        "201"	to "FLUZONE Quadrivalent Southern Hemisphere",
        "202"	to "FLUZONE Quadrivalent Southern Hemisphere",
        "203"	to "MenQuadfi",
        "204"	to "ERVEBO (Ebola Zaire, Live)",
        "205"	to "FLUAD Quadrivalent",
        "206"	to "JYNNEOS",
        "207"	to "Moderna COVID-19",
        "208"	to "Pfizer-BioNTech COVID-19",
        "210"	to "AstraZeneca COVID-19",
        "211"	to "Novavax COVID-19",
        "212"	to "Janssen (J&J) COVID-19",
        "801"	to "AS03 adjuvant"
    )

    private val LOINC_TEST_TYPE = mapOf(
        "LP6464-4" to "Nucleic Acid Amplification w/ Probe",
        "LP217198-3" to "Rapid Immunoassay"
    )

    private val SNOMED_TEST_RESULTS = mapOf(
        "260415000" to "Negative",
        "260373001" to "Positive"
    )

    private val ICD11_MMS = mapOf(
        "XN109" to "SARS-CoV-2",
        "XN0HL" to "SARS-CoV-2 Alpha",
        "XN4Q7" to "SARS-CoV-2 Beta",
        "XN5BQ" to "SARS-CoV-2 Gamma",
        "XN8V6" to "SARS-CoV-2 Delta",
        "XN1GK" to "SARS-CoV-2 Epsilon",
        "XN3ZE" to "SARS-CoV-2 Zeta",
        "XN2V4" to "SARS-CoV-2 Eta",
        "XN4Q1" to "SARS-CoV-2 Theta",
        "XN3UD" to "SARS-CoV-2 Iota",
        "XN9LB" to "SARS-CoV-2 Kappa",
        "XN6AM" to "SARS-CoV-2 Lambda",
        "XN39J" to "SARS-CoV-2 Mu",
        "XN161" to "SARS-CoV-2 Omicron"
    )

    val EXT_BRAND = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand"
    val EXT_MA_HOLDER = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization"
    val EXT_COUNTRY = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination"
    val EXT_VACCINE_VALID = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineValidFrom"

    private fun formatDate(date: DateTimeType?): String? {
        if (date == null) return null
        if (date.value == null) return null
        return fmt.format(date.value)
    }

    // Credential Information

    private fun formatCardTitle(immunization: Immunization?, observation: Observation?): String {
        val scanTime = fmtComplete.format(Calendar.getInstance().time)
            .replace("AM", "am")
            .replace("PM","pm")

        var disease: String? = null;
        var procedure: String? = "Unidentified";

        immunization?.let {
            disease = formatVaccineAgainst(immunization.protocolAppliedFirstRep.targetDisease)
            procedure = "Vaccination"
        }

        observation?.let {
            disease = formatTestAgainst(observation.code)
            procedure = "Test Result"
        }

        if (disease != null && disease!!.isNotEmpty())
            return "$scanTime - $disease $procedure"
        else
            return "$scanTime - $procedure"
    }

    private fun formatValidPeriod(from: Date?, until: Date?): String? {
        return when {
            from != null && until != null -> "Use from " + fmt.format(from) + " to " + fmt.format(until)
            from != null && until == null -> "Use from " + fmt.format(from)
            from == null && until != null -> "Use until " + fmt.format(until)
            else -> null
        }
    }

    // Patient Info
    private fun formatName(names: List<HumanName>?): String? {
        if (names == null || names.isEmpty()) return null
        return names.groupBy { it.text ?: it.nameAsSingleString }.keys.joinToString(", ")
    }

    private fun formatDateWithPrecision(date: DateType): String {
        if (date.precision == TemporalPrecisionEnum.YEAR) {
            return fmtYear.format(date.value)
        } else if (date.precision == TemporalPrecisionEnum.MONTH) {
            return fmtYearMonth.format(date.value)
        } else
            return fmt.format(date.value)
    }

    private fun formatPersonDetails(dob: DateType?, gender: Enumerations.AdministrativeGender?): String? {
        return when {
            dob != null && gender != null -> formatDateWithPrecision(dob) + " - " + gender.display
            dob != null && gender == null -> formatDateWithPrecision(dob)
            dob == null && gender != null -> gender.display
            else -> null
        }
    }

    private fun formatIDs(identifiers: List<Identifier>?): String? {
        if (identifiers == null || identifiers.isEmpty()) return null
        val id = identifiers.groupBy { it.value }.keys.filterNotNull().joinToString(", ")
        if (id.isBlank()) return null
        return "ID: " + id
    }

    // Immunization Info
    private fun isNumber(obj: PositiveIntType?): Boolean {
        return obj != null && obj.value != null
    }

    private fun formatDose(dose: PositiveIntType?, totalDoses: PositiveIntType?): String? {
        return when {
            isNumber(dose) &&  isNumber(totalDoses) -> "Dose: " + dose!!.value + " of " + totalDoses!!.value
            isNumber(dose) && !isNumber(totalDoses) -> "Dose: " + dose!!.value
            !isNumber(dose) && isNumber(totalDoses) -> "Dose: " + "1+ of " + totalDoses!!.value
            else -> null
        }
    }

    private fun formatVaccineAgainst(diseases: List<CodeableConcept>?): String? {
        if (diseases == null || diseases.isEmpty()) return null
        return diseases.groupBy {
            it.coding.groupBy {
                it.display ?: DISEASES[it.code] ?: ICD11_TARGETS[it.code]
            }.keys.joinToString(", ")
        }.keys.joinToString(", ")
    }

    private fun formatVaccineType(vaccines: CodeableConcept?): String? {
        if (vaccines == null) return null
        return vaccines.coding.groupBy {
            it.display ?: VACCINE_PROPH.get(it.code) ?: CVX.get(it.code)
        }.keys.filterNotNull().joinToString(", ")
    }

    private fun formatTestType(types: CodeableConcept?): String? {
        if (types == null) return null
        return formatTestAgainst(types) + " Test Result"
    }

    private fun formatTestTypeDetail(types: CodeableConcept?): String? {
        if (types == null) return null
        return types.coding.filter {
            it.system == "http://loinc.org"
        }.groupBy {
            it.display ?: LOINC_TEST_TYPE.get(it.code)
        }.keys.filterNotNull().joinToString(", ")
    }

    private fun formatTestAgainst(types: CodeableConcept?): String? {
        if (types == null) return null
        return types.coding.filter {
            it.system == "http://id.who.int/icd11/mms"
        }.groupBy {
            it.display ?: ICD11_MMS.get(it.code)
        }.keys.filterNotNull().joinToString(", ")
    }

    private fun formatTestResult(testResult: CodeableConcept?): String? {
        if (testResult == null) return null
        return testResult.coding.groupBy {
            it.display ?: SNOMED_TEST_RESULTS.get(it.code)
        }.keys.filterNotNull().joinToString(", ")
    }

    private fun formatPractioner(practitioner: Practitioner?): String? {
        if (practitioner?.identifier == null) return null
        return practitioner.nameFirstRep?.text ?: practitioner.identifier.groupBy {
                it.value
            }.keys.joinToString(", ")
    }

    private fun formatPractioners(performer: List<Immunization.ImmunizationPerformerComponent>?): String? {
        if (performer == null || performer.isEmpty()) return null
        return performer.filter { it.hasActor() }.groupBy {
            it.actor.display ?: formatPractioner(it.actor.resource as? Practitioner)
        }.keys.joinToString(", ")
    }

    private fun formatOrganization(org: Organization?): String? {
        if (org?.identifier == null) return null
        return org.name ?: org.identifier.groupBy {
            it.value
        }.keys.joinToString(", ")
    }

    private fun formatDueDate(recommendations: List<ImmunizationRecommendation.ImmunizationRecommendationRecommendationComponent>?): String? {
        if (recommendations == null) return null
        return recommendations
            .filter { it.hasDateCriterion() }
            .groupBy {
                it.dateCriterion.groupBy {
                    formatDate(it.valueElement)
                }.keys.joinToString(", ")
            }.keys.joinToString(", ")
    }

    private fun getCodeOrText(obj: Type?): String? {
        if (obj == null) return null
        if (obj is Coding) {
            return obj.display ?: obj.code
        }
        if (obj is CodeableConcept) {
            return obj.coding.groupBy {
                it.display ?: it.code
            }.keys.joinToString(", ")
        }
        return obj.toString()
    }

    private fun getCodeOrText(obj: Reference?): String? {
        if (obj == null) return null
        if (obj.identifier != null && obj.identifier.value != null) {
            return obj.identifier.value
        } else {
            return obj.id
        }
    }

    private fun formatVaccineInfo(lot: String?, brand: String?, manuf: Reference?): String? {
        val lotNumber = lot?.let { "#" + lot }
        val brandStr = brand
        val manufStr = getCodeOrText(manuf)

        return when {
            lotNumber != null && brandStr != null && manufStr != null -> "$brandStr ($lotNumber), $manufStr"

            lotNumber != null && brandStr != null && manufStr == null -> "$brandStr ($lotNumber)"
            lotNumber != null && brandStr == null && manufStr != null -> "$manufStr ($lotNumber)"
            lotNumber == null && brandStr != null && manufStr != null -> "$brandStr, $manufStr"

            lotNumber != null && brandStr == null && manufStr == null -> "Lot $lotNumber"
            lotNumber == null && brandStr != null && manufStr == null -> "$brandStr"
            lotNumber == null && brandStr == null && manufStr != null -> "$manufStr"

            else -> null
        }
    }

    private fun formatLocation(centre: Reference?, country: String?): String? {
       return when {
            centre?.display != null && country != null -> centre.display + ", " + country
            centre?.display != null && country == null -> centre.display
            centre?.display == null && country != null -> country
            else -> null
        }
    }

    private fun formatAuthorizationHolder(extensions: List<Extension>?): String? {
        if (extensions == null || extensions.isEmpty()) return null
        return extensions.groupBy { getCodeOrText(it.value) }.keys.joinToString(", ")
    }

    private fun formatBrand(extensions: List<Extension>?): String? {
        if (extensions == null || extensions.isEmpty()) return null
        return extensions.groupBy { getCodeOrText(it.value) }.keys.joinToString(", ")
    }

    private fun formatVaccineValidTo(extensions: List<Extension>?): String? {
        if (extensions == null || extensions.isEmpty()) return null
        return extensions.groupBy { formatDate(it.value as? DateTimeType) }.keys.joinToString(", ")
    }

    private fun formatCountry(extensions: List<Extension>?): String? {
        if (extensions == null || extensions.isEmpty()) return null
        return extensions.groupBy { getCodeOrText(it.value) }.keys.joinToString(", ")
    }

    fun run(DDCC: Composition): ResultFragment.ResultCard {
        val patient = DDCC.subject.resource as Patient
        val immunization = DDCC.section[0].entry.filter { it.resource.fhirType() == "Immunization" }.firstOrNull()?.resource as? Immunization
        val recommendation = DDCC.section[0].entry.filter { it.resource.fhirType() == "ImmunizationRecommendation" }.firstOrNull()?.resource as? ImmunizationRecommendation
        val testResult = DDCC.section[0].entry.filter { it.resource.fhirType() == "Observation" }.firstOrNull()?.resource as? Observation

        val organization = listOfNotNull(
            immunization?.protocolApplied?.firstOrNull()?.authority?.resource as? Organization,
            (testResult?.encounter?.resource as? Encounter)?.serviceProvider?.resource as? Organization
        ).firstOrNull()

        val location = listOfNotNull(
            formatLocation(immunization?.location, formatCountry(immunization?.getExtensionsByUrl(EXT_COUNTRY))),
            formatLocation((testResult?.encounter?.resource as? Encounter)?.locationFirstRep?.location, formatCountry(testResult?.getExtensionsByUrl(EXT_COUNTRY)))
        ).firstOrNull()

        return ResultFragment.ResultCard(
            DDCC.id,
            formatCardTitle(immunization, testResult),
            formatValidPeriod(DDCC.eventFirstRep.period.start, DDCC.eventFirstRep.period.end),

            // Patient
            formatName(patient.name),
            formatPersonDetails(patient.birthDateElement, patient.gender),
            formatIDs(patient.identifier),

            // Location / Organization / Practitioner
            location,
            formatOrganization(organization),
            formatPractioners(immunization?.performer),

            // Test Results
            formatTestType(testResult?.code),
            formatTestTypeDetail(testResult?.code),
            formatDate(testResult?.effectiveDateTimeType),
            formatTestResult(testResult?.valueCodeableConcept),

            // Immunization
            formatDose(
                immunization?.protocolApplied?.firstOrNull()?.doseNumberPositiveIntType,
                immunization?.protocolApplied?.firstOrNull()?.seriesDosesPositiveIntType
            ),
            formatDate(immunization?.occurrenceDateTimeType),
            formatVaccineValidTo(immunization?.getExtensionsByUrl(EXT_VACCINE_VALID)),
            formatVaccineAgainst(immunization?.protocolApplied?.firstOrNull()?.targetDisease),
            formatVaccineType(immunization?.vaccineCode),
            formatVaccineInfo(
                immunization?.lotNumber,
                formatBrand(immunization?.getExtensionsByUrl(EXT_BRAND)),
                immunization?.manufacturer
            ),
            formatAuthorizationHolder(immunization?.getExtensionsByUrl(EXT_MA_HOLDER)),

            // recommendation
            formatDueDate(recommendation?.recommendation)
        )
    }
}