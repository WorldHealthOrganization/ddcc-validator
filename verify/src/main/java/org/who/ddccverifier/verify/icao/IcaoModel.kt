package org.who.ddccverifier.verify.icao

import com.fasterxml.jackson.annotation.JsonProperty
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.StringType
import org.who.ddccverifier.verify.BaseModel

class IJson(
    val data: Data,
    val sig: Signature,
): BaseModel()

class Data(
    val hdr: Header,
    val msg: Message,
): BaseModel()

class Header(
    @JsonProperty("is")
    val iss: StringType, // Issuer
    val t: StringType,  // Type "icao.test", "icao.vacc",
    val v: IntegerType,      // Version
): BaseModel()

class Message(
    val pid: Patient?,
    // For Vaccination Events
    val uvci: StringType?,
    val ve: List<VaccinationEvent>?,

    // For Test REsults
    val ucti: StringType?,
    val sp: ServiceProvider?,
    val dat: DateTimeTestReport?,
    val tr: TestResult?,
    val opt: StringType?, // Optional DataField
): BaseModel()

class Patient(
    val dob: StringType?,
    val i: StringType?, // Identifier (Passport Number)
    val n: StringType?, // Name
    val sex: StringType?, // Doc 9303-4 Section 4.1.1.1 – Visual Inspection Zone M or F)
    val dt: StringType?, // Document Type:
    // P – Passport (Doc 9303-4)
    // A – ID Card (Doc 9303-5)
    // C – ID Card (Doc 9303-5)
    // I – ID Card Doc 9303-5)
    // AC - Crew Member Certificate (Doc 9303-5)
    // V – Visa (Doc 9303-7)
    // D – Driving License (ISO18013-1)
    val dn: StringType?, // Document Number
    val ai: StringType?, // Additional Identifier
): BaseModel()

class VaccinationEvent(
    val des: StringType?,  // Prophilaxis // (http://id.who.int/icd/entity/164949870)
    val dis: StringType?,  // Diesease or Agent Targeted (ICD-11)
    val nam: StringType?,  // Vaccine Brand
    val vd: List<VaccinationDetails>?,
): BaseModel()

class VaccinationDetails(
    val adm: StringType?,  // Administering Center
    val ctr: StringType?,  // Country AUS
    val dvc: DateTimeType?,  // Date of Vaccination
    val lot: StringType?,  // Lot #
    val seq: PositiveIntType?,      // Dose Sequence
): BaseModel()

class ServiceProvider(
    val spn: StringType?,  // Name of the Service Provider
    val ctr: StringType?,  // Country of the Test
    val cd: Contact?,  // Contact Info
): BaseModel()

class Contact (
    val p: StringType?, // phone
    val e: StringType?, // email
    val a: StringType?, // address
): BaseModel()

class DateTimeTestReport(
    val sc: StringType?,  // Specimen Collection Time
    val ri: StringType?,  // Report Issuance Time
): BaseModel()

class TestResult(
    val tc: StringType?,  // Test Type (molecular(PCR), molecular(other), antigen, antibody)
    val r: StringType?,  // Results (positive, negative, normal, abnormal
    val m: StringType?,  // Sampling Method nasopharyngeal, oropharyngeal, saliva, blood, other
): BaseModel()

class Signature(
    val alg: StringType,
    val cer: StringType,
    val sigvl: StringType,
): BaseModel()