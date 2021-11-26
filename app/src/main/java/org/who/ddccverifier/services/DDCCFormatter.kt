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

    private fun formatPersonDetails(dob: Date?, gender: Enumerations.AdministrativeGender?): String? {
        return when {
            dob != null && gender != null -> fmt.format(dob) + " - " + gender.display
            dob != null && gender == null -> fmt.format(dob)
            dob == null && gender != null -> gender.display
            else -> null
        }
    }

    private fun isNumber(obj: PositiveIntType?): Boolean {
        return obj != null && obj.value != null
    }

    private fun formatDose(dose: PositiveIntType?, totalDoses: PositiveIntType?): String? {
        return when {
            isNumber(dose) &&  isNumber(totalDoses) -> "Dose: " + dose!!.value + " of " + totalDoses!!.value
            isNumber(dose) && !isNumber(totalDoses) -> "Dose: " + dose!!.value
            !isNumber(dose) &&  isNumber(totalDoses) -> "Dose: " + "1+ of " + totalDoses!!.value
            else -> null
        }
    }

    private fun formatCardTitle(targetDisease: Coding?): String {
        val scanTime = fmtComplete.format(Calendar.getInstance().time).replace("AM", "am").replace("PM","pm")
        val disease = formatVaccineAgainst(targetDisease)
        val procedure = "Vaccination"

        return "$scanTime - $disease $procedure"
    }

    private fun formatVaccineAgainst(disease: Coding?): String? {
        if (disease == null) return null
        if (disease.code == null) return null
        return DISEASES.get(disease.code)
    }

    private fun formatVaccineType(vaccine: Coding?): String? {
        if (vaccine == null) return null
        if (vaccine.code == null) return null
        return VACCINE_PROPH.get(vaccine.code)
    }

    private fun formatDate(date: DateTimeType?): String? {
        if (date == null) return null
        if (date.value == null) return null
        return fmt.format(date.value)
    }

    private fun formatValidPeriod(from: Date?, until: Date?): String? {
        return when {
            from != null && until != null -> "Valid from " + fmt.format(from) + " to " + fmt.format(until)
            from != null && until == null -> "Valid from " + fmt.format(from)
            from == null && until != null -> "Valid until " + fmt.format(until)
            else -> null
        }
    }

    val EXT_BRAND = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand"
    val EXT_MA_HOLDER = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization"
    val EXT_COUNTRY = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination"
    val EXT_VACCINE_VALID = "https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineValidFrom"

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

    private fun formatVaccineInfo(lot: String?, brand: Type?, manuf: Reference?): String? {
        val lotNumber = lot?.let { "#" + lot }
        val brandStr = getCodeOrText(brand)
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

    private fun formatLocation(centre: Reference?, country: Type?): String? {
        return when {
            centre != null && country != null -> centre.display + ", " + getCodeOrText(country)
            centre != null && country == null -> centre.display
            centre == null && country != null -> getCodeOrText(country)
            else -> null
        }
    }

    fun run(DDCC: Composition): ResultFragment.ResultCard {
        val patient = DDCC.subject.resource as Patient
        val immunization = DDCC.section[0].entry.filter { it.resource.fhirType() == "Immunization" }.first()?.resource as? Immunization
        val recommendation = DDCC.section[0].entry.filter { it.resource.fhirType() == "ImmunizationRecommendation" }.firstOrNull()?.resource as? ImmunizationRecommendation

        return ResultFragment.ResultCard(
            formatCardTitle(immunization?.protocolAppliedFirstRep?.targetDiseaseFirstRep?.codingFirstRep),
            patient.name[0].text,
            formatPersonDetails(patient.birthDate, patient.gender),
            formatDose(immunization?.protocolAppliedFirstRep?.doseNumberPositiveIntType, immunization?.protocolAppliedFirstRep?.seriesDosesPositiveIntType),
            formatDate(immunization?.occurrenceDateTimeType),
            formatDate(recommendation?.recommendationFirstRep?.dateCriterionFirstRep?.valueElement),
            formatDate(immunization?.getExtensionsByUrl(EXT_VACCINE_VALID)?.firstOrNull()?.value as? DateTimeType),
            formatVaccineAgainst(immunization?.protocolAppliedFirstRep?.targetDiseaseFirstRep?.codingFirstRep),
            formatVaccineType(immunization?.vaccineCode?.codingFirstRep),
            formatVaccineInfo(immunization?.lotNumber, immunization?.getExtensionsByUrl(EXT_BRAND)?.firstOrNull()?.value, immunization?.manufacturer),
            getCodeOrText(immunization?.getExtensionsByUrl(EXT_MA_HOLDER)?.firstOrNull()?.value),
            formatLocation(immunization?.location, immunization?.getExtensionsByUrl(EXT_COUNTRY)?.firstOrNull()?.value),
            DDCC.id,
            (immunization?.protocolApplied?.firstOrNull()?.authority?.resource as? Organization)?.identifierFirstRep?.value,
            patient.identifierFirstRep?.value,
            (immunization?.performer?.firstOrNull()?.actor?.resource as? Practitioner)?.identifierFirstRep?.value,
            formatValidPeriod(DDCC.event[0].period.start, DDCC.event[0].period.end),
        )
    }
}