package org.who.ddccverifier.services

import com.upokecenter.cbor.CBORObject
import org.who.ddccverifier.views.ResultFragment
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DDCCFormatter {
    // Using old java.time to keep compatibility down to Android SDK 22.
    private var dfISO: DateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val fmt = SimpleDateFormat("MMM d, yyyy")

    private val fmtComplete = SimpleDateFormat("MMM d, h:mma")

    private fun parseDate(date: CBORObject?): Date? {
        if (date == null) return null
        if (date.AsString().isEmpty()) return null
        return dfISO.parse(date.AsString())
    }

    private fun label(label: String, text: String?): String? {
        if (text == null || text.isEmpty()) return null
        return "$label: $text"
    }

    private fun formatText(text: CBORObject?): String? {
        if (text == null) return null
        if (text.AsString().isEmpty()) return null
        return text.AsString()
    }

    private fun formatPersonDetails(dob: CBORObject?, gender: CBORObject?): String? {
        when {
            dob != null && gender != null -> return "" + fmt.format(parseDate(dob)) + " - " + GENDERS[gender["code"].AsString()]
            dob != null && gender == null -> return "" + fmt.format(parseDate(dob))
            dob == null && gender != null -> return "" + GENDERS[gender["code"].AsString()]
            else -> return null
        }
    }

    private fun formatDoseDate(date: CBORObject?): String? {
        return fmt.format(parseDate(date))
    }

    private fun formatNextDose(date: CBORObject?): String? {
        return fmt.format(parseDate(date))
    }

    private fun formatVaccineValid(date: CBORObject?): String? {
        return fmt.format(parseDate(date))
    }

    private fun formatValidPeriod(from: CBORObject?, until: CBORObject?): String? {
        when {
            from != null && until != null -> return "Valid from " + fmt.format(parseDate(from)) + " to " + fmt.format(parseDate(until))
            from != null && until == null -> return "Valid from " + fmt.format(parseDate(from))
            from == null && until != null -> return "Valid until " + fmt.format(parseDate(until))
            else -> return null
        }
    }

    private fun formatDose(dose: CBORObject?, totalDoses: CBORObject?): String? {
        when {
            dose != null && totalDoses != null -> return "Dose: " + dose.AsInt32() + " of " + totalDoses.AsInt32()
            dose != null && totalDoses == null -> return "Dose: " + dose.AsInt32()
            dose == null && totalDoses != null -> return "Dose: " + "1+ of " + totalDoses.AsInt32()
            else -> return null
        }
    }

    val DISEASES = mapOf("840539006" to "COVID-19")
    val VACCINE_PROPH = mapOf(
        "1119349007" to "SARS-CoV-2 mRNA Vaccine" ,
        "1119305005" to "SARS-CoV-2 Antigen Vaccine",
        "J07BX03" to "COVID-19 Vaccine"
    )
    val GENDERS = mapOf(
        "male" to "Male" ,
        "female" to "Female",
        "other" to "Other"
    )

    private fun formatVaccineAgainst(disease: CBORObject?): String? {
        val diseaseDesc = disease?.get("code")?.AsString();
        return DISEASES.get(diseaseDesc);
    }

    private fun formatVaccineType(vaccine: CBORObject?): String? {
        return VACCINE_PROPH.get(vaccine?.get("code")?.AsString())
    }

    private fun formatLocation(centre: CBORObject?, country: CBORObject?): String? {
        when {
            centre != null && country != null -> return centre.AsString() + ", " + country.get("code").AsString()
            centre != null && country == null -> return centre.AsString()
            centre == null && country != null -> return country.get("code").AsString()
            else -> return null
        }
    }

    private fun formatLotNumber(lot: CBORObject?): String? {
        if (lot == null) return null
        return "#"+formatText(lot);
    }

    private fun formatVaccineInfo(lot: CBORObject?, brand: CBORObject?, manuf: CBORObject?): String? {
        val lotNumber = formatLotNumber(lot);
        val brandStr = formatText(brand?.get("code"));
        val manufStr = formatText(manuf?.get("code"));

        when {
            lotNumber != null && brandStr != null && manufStr != null -> return "$brandStr ($lotNumber), $manufStr"

            lotNumber != null && brandStr != null && manufStr == null -> return "$brandStr ($lotNumber)"
            lotNumber != null && brandStr == null && manufStr != null -> return "$manufStr ($lotNumber)"
            lotNumber == null && brandStr != null && manufStr != null -> return "$brandStr, $manufStr"

            lotNumber != null && brandStr == null && manufStr == null -> return "Lot $lotNumber"
            lotNumber == null && brandStr != null && manufStr == null -> return "$brandStr"
            lotNumber == null && brandStr == null && manufStr != null -> return "$manufStr"

            else -> return null
        }
    }

    private fun formatVaccineInfo2(ma: CBORObject?): String? {
        return formatText(ma?.get("code"))
    }

    private fun formatHCID(text: CBORObject?): String? {
        return formatText(text)
    }

    private fun formatPHA(text: CBORObject?): String? {
        return formatText(text)
    }

    private fun formatIdentifier(text: CBORObject?): String? {
        return formatText(text)
    }

    private fun formatHW(text: CBORObject?): String? {
        return formatText(text)
    }

    private fun formatCardTitle(targetDisease: CBORObject?): String? {
        val scanTime = fmtComplete.format(Calendar.getInstance().time).replace("AM", "am").replace("PM","pm")
        val disease = formatVaccineAgainst(targetDisease)
        val procedure = "Vaccination"

        return "$scanTime - $disease $procedure"
    }

    fun run(DDCC: CBORObject): ResultFragment.ResultCard {
        return ResultFragment.ResultCard(
            formatCardTitle(DDCC["disease"]),
            formatText(DDCC["name"]),
            formatPersonDetails(DDCC["birthDate"], DDCC["sex"]),
            formatDose(DDCC["dose"], DDCC["total_doses"]),
            formatDoseDate(DDCC["date"]),
            formatNextDose(DDCC["due_date"]),
            formatVaccineValid(DDCC["vaccine_valid"]),
            formatVaccineAgainst(DDCC["disease"]),
            formatVaccineType(DDCC["vaccine"]),
            formatVaccineInfo(DDCC["lot"], DDCC["brand"], DDCC["manufacturer"]),
            formatVaccineInfo2(DDCC["ma_holder"]),
            formatLocation(DDCC["centre"], DDCC["country"]),
            formatHCID(DDCC["hcid"]),
            formatPHA(DDCC["pha"]),
            formatIdentifier(DDCC["identifier"]),
            formatHW(DDCC["hw"]),
            formatValidPeriod(DDCC["valid_from"], DDCC["valid_until"]),
        )
    }
}