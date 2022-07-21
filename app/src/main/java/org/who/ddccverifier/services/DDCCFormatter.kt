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
        "LP217198-3" to "Rapid Immunoassay",

        "71776-9" to "Gamma interferon background [Units/volume] in Blood by Immunoassay",
        "82163-7" to "Human coronavirus 229E RNA [Presence] in Nasopharynx by NAA with non-probe detection",
        "88891-7" to "Human coronavirus 229E+HKU1+OC43+NL63 RNA [Presence] in Nasopharynx by NAA with non-probe detection",
        "82161-1" to "Human coronavirus HKU1 RNA [Presence] in Nasopharynx by NAA with non-probe detection",
        "82162-9" to "Human coronavirus NL63 RNA [Presence] in Nasopharynx by NAA with non-probe detection",
        "82164-5" to "Human coronavirus OC43 RNA [Presence] in Nasopharynx by NAA with non-probe detection",
        "80382-5" to "Influenza virus A Ag [Presence] in Nasopharynx by Rapid immunoassay",
        "95942-9" to "Influenza virus A and B and SARS-CoV+SARS-CoV-2 (COVID-19) Ag panel - Upper respiratory specimen by Rapid immunoassay",
        "95941-1" to "Influenza virus A and B and SARS-CoV-2 (COVID-19) and Respiratory syncytial virus RNA panel - Respiratory specimen by NAA with probe detection",
        "95380-2" to "Influenza virus A and B and SARS-CoV-2 (COVID-19) and SARS-related CoV RNA panel - Respiratory specimen by NAA with probe detection",
        "95423-0" to "Influenza virus A and B and SARS-CoV-2 (COVID-19) identified in Respiratory specimen by NAA with probe detection",
        "95422-2" to "Influenza virus A and B RNA and SARS-CoV-2 (COVID-19) N gene panel - Respiratory specimen by NAA with probe detection",
        "92142-9" to "Influenza virus A RNA [Presence] in Respiratory specimen by NAA with probe detection",
        "80383-3" to "Influenza virus B Ag [Presence] in Nasopharynx by Rapid immunoassay",
        "92141-1" to "Influenza virus B RNA [Presence] in Respiratory specimen by NAA with probe detection",
        "71774-4" to "Mitogen stimulated gamma interferon [Units/volume] corrected for background in Blood",
        "71772-8" to "Mitogen stimulated gamma interferon [Units/volume] in Blood",
        "92131-2" to "Respiratory syncytial virus RNA [Presence] in Respiratory specimen by NAA with probe detection",
        "95971-8" to "SARS CoV-2 stimulated gamma interferon [Presence] in Blood",
        "95974-2" to "SARS CoV-2 stimulated gamma interferon panel - Blood",
        "95972-6" to "SARS CoV-2 stimulated gamma interferon release by T-cells [Units/volume] corrected for background in Blood",
        "95973-4" to "SARS CoV-2 stimulated gamma interferon release by T-cells [Units/volume] in Blood",
        "95209-3" to "SARS-CoV + SARS-Cov-2 (COVID19) Ag [Presence] in Respiratory specimen by Rapid immunoassay",
        "94763-0" to "SARS-CoV-2 (COVID19) [Presence] in Unspecified specimen by Organism specific culture",
        "94661-6" to "SARS-CoV-2 (COVID19) Ab [Interpretation] in Serum or Plasma",
        "95825-6" to "SARS-CoV-2 (COVID-19) Ab [Presence] in DBS by Immunoassay",
        "94762-2" to "SARS-CoV-2 (COVID19) Ab [Presence] in Serum or Plasma by Immunoassay",
        "94769-7" to "SARS-CoV-2 (COVID19) Ab [Units/volume] in Serum or Plasma by Immunoassay",
        "96118-5" to "SARS-CoV-2 (COVID-19) Ab panel - DBS by Immunoassay",
        "94558-4" to "SARS-CoV-2 (COVID19) Ag [Presence] in Respiratory specimen by Rapid immunoassay",
        "96119-3" to "SARS-CoV-2 (COVID-19) Ag [Presence] in Upper respiratory specimen by Immunoassay",
        "96094-8" to "SARS-CoV-2 (COVID-19) and SARS-related CoV RNA panel - Respiratory specimen by NAA with probe detection",
        "94509-7" to "SARS-CoV-2 (COVID19) E gene [Cycle Threshold #] in Unspecified specimen by NAA with probe detection",
        "94758-0" to "SARS-CoV-2 (COVID19) E gene [Presence] in Respiratory specimen by NAA with probe detection",
        "94765-5" to "SARS-CoV-2 (COVID19) E gene [Presence] in Serum or Plasma by NAA with probe detection",
        "94315-9" to "SARS-CoV-2 (COVID19) E gene [Presence] in Unspecified specimen by NAA with probe detection",
        "95125-1" to "SARS-CoV-2 (COVID19) IgA + IgM [Presence] in Serum or Plasma by Immunoassay",
        "94562-6" to "SARS-CoV-2 (COVID19) IgA Ab [Presence] in Serum or Plasma by Immunoassay",
        "94768-9" to "SARS-CoV-2 (COVID19) IgA Ab [Presence] in Serum, Plasma, or Blood by Rapid imunoassay",
        "95427-1" to "SARS-CoV-2 (COVID-19) IgA Ab [Titer] in Serum or Plasma by Immunofluorescence",
        "94720-0" to "SARS-CoV-2 (COVID19) IgA Ab [Units/volume] in Serum or Plasma by Immunoassay",
        "94761-4" to "SARS-CoV-2 (COVID19) IgG Ab [Presence] in DBS by Immunoassay",
        "94563-4" to "SARS-CoV-2 (COVID19) IgG Ab [Presence] in Serum or Plasma by Immunoassay",
        "94507-1" to "SARS-CoV-2 (COVID-19) IgG Ab [Presence] in Serum, Plasma or Blood by Rapid immunoassay",
        "95429-7" to "SARS-CoV-2 (COVID-19) IgG Ab [Titer] in Serum or Plasma by Immunofluorescence",
        "94505-5" to "SARS-CoV-2 (COVID19) IgG Ab [Units/volume] in Serum or Plasma by Immunoassay",
        "94504-8" to "SARS-CoV-2 (COVID19) IgG and IgM panel - Serum or Plasma by Immunoassay",
        "94503-0" to "SARS-CoV-2 (COVID19) IgG and IgM panel - Serum or Plasma Qualitative by Rapid immunoassay",
        "94547-7" to "SARS-CoV-2 (COVID19) IgG+IgM Ab [Presence] in Serum or Plasma by Immunoassay",
        "95542-7" to "SARS-CoV-2 (COVID-19) IgG+IgM Ab [Presence] in Serum, Plasma or Blood by Rapid immunoassay",
        "95416-4" to "SARS-CoV-2 (COVID-19) IgM Ab [Presence] in DBS by Immunoassay",
        "94564-2" to "SARS-CoV-2 (COVID19) IgM Ab [Presence] in Serum or Plasma by Immunoassay",
        "94508-9" to "SARS-CoV-2 (COVID19) IgM Ab [Presence] in Serum or Plasma by Rapid immunoassay",
        "95428-9" to "SARS-CoV-2 (COVID-19) IgM Ab [Titer] in Serum or Plasma by Immunofluorescence",
        "94506-3" to "SARS-CoV-2 (COVID19) IgM Ab [Units/volume] in Serum or Plasma by Immunoassay",
        "95521-1" to "SARS-CoV-2 (COVID-19) N gene [#/volume] (viral load) in Respiratory specimen by NAA with probe detection",
        "94510-5" to "SARS-CoV-2 (COVID19) N gene [Cycle Threshold #] in Unspecified specimen by NAA with probe detection",
        "94311-8" to "SARS-CoV-2 (COVID19) N gene [Cycle Threshold #] in Unspecified specimen by Nucleic acid amplification using primer-probe set N1",
        "94312-6" to "SARS-CoV-2 (COVID19) N gene [Cycle Threshold #] in Unspecified specimen by Nucleic acid amplification using primer-probe set N2",
        "95522-9" to "SARS-CoV-2 (COVID-19) N gene [Log #/volume] (viral load) in Respiratory specimen by NAA with probe detection",
        "94760-6" to "SARS-CoV-2 (COVID19) N gene [Presence] in Nasopharynx by NAA with probe detection",
        "95409-9" to "SARS-CoV-2 (COVID-19) N gene [Presence] in Nose by NAA with probe detection",
        "94533-7" to "SARS-CoV-2 (COVID19) N gene [Presence] in Respiratory specimen by NAA with probe detection",
        "94757-2" to "SARS-CoV-2 (COVID19) N gene [Presence] in Respiratory specimen by Nucleaic acid amplification using CDC primer-probe set N2",
        "94756-4" to "SARS-CoV-2 (COVID19) N gene [Presence] in Respiratory specimen by Nucleic acid amplification using CDC primer-probe set N1",
        "95425-5" to "SARS-CoV-2 (COVID-19) N gene [Presence] in Saliva (oral fluid) by NAA with probe detection",
        "96448-6" to "SARS-CoV-2 (COVID-19) N gene [Presence] in Saliva (oral fluid) by Nucleic acid amplification using CDC primer-probe set N1",
        "94766-3" to "SARS-CoV-2 (COVID19) N gene [Presence] in Serum or Plasma by NAA with probe detection",
        "94316-7" to "SARS-CoV-2 (COVID19) N gene [Presence] in Unspecified specimen by NAA with probe detection",
        "94307-6" to "SARS-CoV-2 (COVID19) N gene [Presence] in Unspecified specimen by Nucleic acid amplification using primer-probe set N1",
        "94308-4" to "SARS-CoV-2 (COVID19) N gene [Presence] in Unspecified specimen by Nucleic acid amplification using primer-probe set N2",
        "95411-5" to "SARS-CoV-2 (COVID-19) neutralizing antibody [Presence] in Serum by pVNT",
        "95410-7" to "SARS-CoV-2 (COVID-19) neutralizing antibody [Titer] in Serum by pVNT",
        "94644-2" to "SARS-CoV-2 (COVID19) ORF1ab region [Cycle Threshold #] in Respiratory specimen by NAA with probe detection",
        "94511-3" to "SARS-CoV-2 (COVID19) ORF1ab region [Cycle Threshold #] in Unspecified specimen by NAA with probe detection",
        "94559-2" to "SARS-CoV-2 (COVID19) ORF1ab region [Presence] in Respiratory specimen by NAA with probe detection",
        "95824-9" to "SARS-CoV-2 (COVID-19) ORF1ab region [Presence] in Saliva (oral fluid) by NAA with probe detection",
        "94639-2" to "SARS-CoV-2 (COVID19) ORF1ab region [Presence] in Unspecified specimen by NAA with probe detection",
        "94646-7" to "SARS-CoV-2 (COVID19) RdRp gene [Cycle Threshold #] in Respiratory specimen by NAA with probe detection",
        "94645-9" to "SARS-CoV-2 (COVID19) RdRp gene [Cycle Threshold #] in Unspecified specimen by NAA with probe detection",
        "96120-1" to "SARS-CoV-2 (COVID-19) RdRp gene [Presence] in Lower respiratory specimen by NAA with probe detection",
        "94534-5" to "SARS-CoV-2 (COVID19) RdRp gene [Presence] in Respiratory specimen by NAA with probe detection",
        "96091-4" to "SARS-CoV-2 (COVID-19) RdRp gene [Presence] in Saliva (oral fluid) by NAA with probe detection",
        "94314-2" to "SARS-CoV-2 (COVID19) RdRp gene [Presence] in Unspecified specimen by NAA with probe detection",
        "96123-5" to "SARS-CoV-2 (COVID-19) RdRp gene [Presence] in Upper respiratory specimen by NAA with probe detection",
        "94745-7" to "SARS-CoV-2 (COVID19) RNA [Cycle Threshold#] in Respiratory specimen by NAA with probe detection",
        "94746-5" to "SARS-CoV-2 (COVID19) RNA [Cycle Threshold#] in Unspecified specimen by NAA with probe detection",
        "94819-0" to "SARS-CoV-2 (COVID19) RNA [Log#/volume] (viral load) in Unspecified specimen by NAA with probe detection",
        "94565-9" to "SARS-CoV-2 (COVID19) RNA [Presence] in Nasopharynx by NAA with non-probe detection",
        "94759-8" to "Sars-CoV-2 (COVID19) RNA [Presence] in Nasopharynx by NAA with probe detection",
        "95406-5" to "SARS-CoV-2 (COVID-19) RNA [Presence] in Nose by NAA with probe detection",
        "95608-6" to "SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by NAA with non-probe detection",
        "94500-6" to "SARS-CoV-2 (COVID19) RNA [Presence] in Respiratory specimen by NAA with probe detection",
        "95424-8" to "SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by Sequencing",
        "94845-5" to "SARS-CoV-2 (COVID19) RNA [Presence] in Saliva (oral fluid) by NAA with probe",
        "94822-4" to "SARS-CoV-2 (COVID19) RNA [Presence] in Saliva (oral fluid) by Sequencing",
        "94660-8" to "SARS-CoV-2 (COVID19) RNA [Presence] in Serum or Plasma by NAA with probe detection",
        "94309-2" to "SARS-CoV-2 (COVID19) RNA [Presence] in Unspecified specimen by NAA with probe detection",
        "94531-1" to "SARS-CoV-2 (COVID19) RNA panel - Respiratory specimen by NAA with probe detection",
        "95826-4" to "SARS-CoV-2 (COVID-19) RNA panel - Saliva (oral fluid) by NAA with probe detection",
        "94306-8" to "SARS-Cov-2 (COVID19) RNA panel - Unspecified specimen by NAA with probe detection",
        "94642-6" to "SARS-CoV-2 (COVID19) S gene [Cycle Threshold #] in Respiratory specimen by NAA with probe detection",
        "94643-4" to "SARS-CoV-2 (COVID19) S gene [Cycle Threshold #] in Unspecified specimen by NAA with probe detection",
        "94640-0" to "SARS-CoV-2 (COVID19) S gene [Presence] in Respiratory specimen by NAA with probe detection",
        "95609-4" to "SARS-CoV-2 (COVID-19) S gene [Presence] in Respiratory specimen by Sequencing",
        "94767-1" to "SARS-CoV-2 (COVID19) S gene [Presence] in Serum or Plasma by NAA with probe detection",
        "94641-8" to "SARS-CoV-2 (COVID19) S gene [Presence] in Unspecified specimen by NAA with probe detection",
        "96603-6" to "SARS-CoV-2 (COVID-19) S protein RBD neutralizing antibody [Presence] in Serum or Plasma by Immunoassay",
        "95970-0" to "SARS-CoV-2 (COVID-19) specific TCRB gene rearrangements [Presence] in Blood by Sequencing",
        "94764-8" to "SARS-coV-2 (COVID19) whole genome [Nucleiotide sequence] in Isolate by Sequencing",
        "94313-4" to "SARS-like coronavirus N gene [Cycle Threshold #] in Unspecified specimen by NAA with probe detection",
        "94310-0" to "SARS-like coronavirus N gene [Presence] in Unspecified specimen by NAA with probe detection",
        "96121-9" to "SARS-related coronavirus E gene [Presence] in Lower respiratory specimen by NAA with probe detection",
        "95823-1" to "SARS-related coronavirus E gene [Presence] in Saliva (oral fluid) by NAA with probe detection",
        "96122-7" to "SARS-related coronavirus E gene [Presence] in Upper respiratory specimen by NAA with probe detection",
        "94502-2" to "SARS-related coronavirus RNA [Presence] in Respiratory specimen by NAA with probe detection",
        "94647-5" to "SARS-related coronavirus RNA [Presence] in Unspecified specimen by NAA with probe detection",
        "94532-9" to "SARS-related coronavirus+MERS coronavirus RNA [Presence] in Respiratory specimen by NAA with probe detection"
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
        var disease = formatTestAgainst(types);
        if (disease != null && disease.isNotEmpty())
            return "$disease Test Result"
        else
            return "Test Result"
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