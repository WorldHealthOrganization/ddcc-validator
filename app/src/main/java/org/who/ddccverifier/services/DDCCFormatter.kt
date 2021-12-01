package org.who.ddccverifier.services

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.views.ResultFragment
import java.text.SimpleDateFormat
import java.util.*

class DDCCFormatter {
    private val fmt = SimpleDateFormat("MMM d, yyyy")
    private val fmtComplete = SimpleDateFormat("MMM d, h:mma")

    private val DISEASES = mapOf("840539006" to "COVID-19")
    private val VACCINE_PROPH = mapOf(
        "1119349007" to "SARS-CoV-2 mRNA Vaccine" ,
        "1119305005" to "SARS-CoV-2 Antigen Vaccine",
        "J07BX03" to "COVID-19 Vaccine"
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

    private fun formatCardTitle(targetDisease: List<CodeableConcept>?): String {
        val scanTime = fmtComplete.format(Calendar.getInstance().time)
                                  .replace("AM", "am")
                                  .replace("PM","pm")
        val disease = formatVaccineAgainst(targetDisease)
        val procedure = "Vaccination"

        return "$scanTime - $disease $procedure"
    }

    private fun formatValidPeriod(from: Date?, until: Date?): String? {
        return when {
            from != null && until != null -> "Valid from " + fmt.format(from) + " to " + fmt.format(until)
            from != null && until == null -> "Valid from " + fmt.format(from)
            from == null && until != null -> "Valid until " + fmt.format(until)
            else -> null
        }
    }

    // Patient Info
    private fun formatName(names: List<HumanName>): String? {
        return names.groupBy { it.text }.keys.joinToString(", ")
    }

    private fun formatPersonDetails(dob: Date?, gender: Enumerations.AdministrativeGender?): String? {
        return when {
            dob != null && gender != null -> fmt.format(dob) + " - " + gender.display
            dob != null && gender == null -> fmt.format(dob)
            dob == null && gender != null -> gender.display
            else -> null
        }
    }

    private fun formatIDs(identifiers: List<Identifier>): String? {
        return "ID: " + identifiers.groupBy { it.value }.keys.joinToString(", ")
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
        if (diseases == null) return null
        return diseases.groupBy {
            it.coding.groupBy {
                DISEASES.get(it.code)
            }.keys.joinToString(", ")
        }.keys.joinToString(", ")
    }

    private fun formatVaccineType(vaccines: CodeableConcept?): String? {
        if (vaccines == null) return null
        return vaccines.coding.groupBy {
            VACCINE_PROPH.get(it.code)
        }.keys.joinToString(", ")
    }

    private fun formatPractioner(practitioner: Practitioner?): String? {
        if (practitioner?.identifier == null) return null
        return practitioner?.identifier.groupBy {
            it.value
        }.keys.joinToString(", ")
    }

    private fun formatPractioners(performer: List<Immunization.ImmunizationPerformerComponent>?): String? {
        if (performer == null || performer.isEmpty()) return null
        return performer.filter { it.hasActor() }.groupBy {
            formatPractioner(it.actor.resource as? Practitioner)
        }.keys.joinToString(", ")
    }

    private fun formatOrganization(org: Organization?): String? {
        if (org?.identifier == null) return null
        return org?.identifier.groupBy {
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
            return obj.code
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
            centre != null && country != null -> centre.display + ", " + country
            centre != null && country == null -> centre.display
            centre == null && country != null -> country
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

    private fun formatStatus(completedImmunization: Boolean): String {
        if (completedImmunization) {
            return "COVID Safe"
        }
        return "COVID Vulnerable"
    }

    fun run(DDCC: Composition, completedImmunization: Boolean): ResultFragment.ResultCard {
        val patient = DDCC.subject.resource as Patient
        val immunization = DDCC.section[0].entry.filter { it.resource.fhirType() == "Immunization" }.firstOrNull()?.resource as? Immunization
        val recommendation = DDCC.section[0].entry.filter { it.resource.fhirType() == "ImmunizationRecommendation" }.firstOrNull()?.resource as? ImmunizationRecommendation

        return ResultFragment.ResultCard(
            DDCC.id,
            formatCardTitle(immunization?.protocolApplied?.firstOrNull()?.targetDisease),
            formatValidPeriod(DDCC.event[0].period.start, DDCC.event[0].period.end),
            formatName(patient.name),
            formatPersonDetails(patient.birthDate, patient.gender),
            formatIDs(patient.identifier),
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
            formatLocation(immunization?.location, formatCountry(immunization?.getExtensionsByUrl(EXT_COUNTRY))),
            formatOrganization((immunization?.protocolApplied?.firstOrNull()?.authority?.resource as? Organization)),
            formatPractioners(immunization?.performer),
            formatDueDate(recommendation?.recommendation),
            formatStatus(completedImmunization)
        )
    }
}