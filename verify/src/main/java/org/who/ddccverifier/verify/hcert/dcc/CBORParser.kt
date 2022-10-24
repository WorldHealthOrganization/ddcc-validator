package org.who.ddccverifier.verify.hcert.dcc

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.upokecenter.cbor.CBORObject
import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.verify.hcert.dcc.logical.*
import java.util.*

class CBORParser {
    val CWT_ISSUER = 1
    val CWT_SUBJECT = 2
    val CWT_AUDIENCE = 3
    val CWT_EXPIRATION = 4
    val CWT_NOT_BEFORE = 5
    val CWT_ISSUED_AT = 6
    val CWT_ID = 7
    val HCERT = -260
    val HCERT_V1 = 1
    //val UY_STRING_PAYLOAD = 99

    private fun parseString(str: CBORObject?): StringType? {
        if (str == null || str.isUndefined) return null
        return StringType(str.AsString())
    }

    private fun parseInteger(integer: CBORObject?): IntegerType? {
        if (integer == null || integer.isUndefined) return null
        return IntegerType(integer.AsInt32Value())
    }

    private fun parsePositiveIntType(positiveInt: CBORObject?): PositiveIntType? {
        if (positiveInt == null || positiveInt.isUndefined) return null
        return PositiveIntType().apply {
            value = positiveInt.AsInt32Value()
        }
    }

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
            if (date.AsDouble().isNaN()) return null
            DateTimeType(Date(date.AsInt64()*1000), TemporalPrecisionEnum.DAY)
        } else {
            DateTimeType(date.AsString())
        }
    }

    fun parseVaccinations(vArray: CBORObject?): List<Vaccination>? {
        var vacList = mutableListOf<Vaccination>()
        if (vArray != null && !vArray.isUndefined) {
            for (i in 0 until vArray.size()) {
                val vax = vArray[i]
                vacList.add(Vaccination(
                    parseString(vax["tg"]),
                    parseString(vax["vp"]),
                    parseString(vax["mp"]),
                    parseString(vax["ma"]),
                    parsePositiveIntType(vax["dn"]),
                    parsePositiveIntType(vax["sd"]),
                    parseDateTimeType(vax["dt"]),
                    parseString(vax["co"]),
                    parseString(vax["is"]),
                    parseString(vax["ci"])
                ))
            }
        }
        return vacList
    }

    fun parseTests(tArray: CBORObject?): List<Test>? {
        var testList = mutableListOf<Test>()
        if (tArray != null && !tArray.isUndefined) {
            for (i in 0 until tArray.size()) {
                val test = tArray[i]
                testList.add(Test(
                    parseString(test["tg"]),
                    parseString(test["tt"]),
                    parseString(test["mn"]),
                    parseString(test["ma"]),
                    parseDateTimeType(test["sc"]),
                    parseString(test["tr"]),
                    parseString(test["tc"]),
                    parseString(test["co"]),
                    parseString(test["is"]),
                    parseString(test["ci"])
                ))
            }
        }
        return testList
    }

    fun parseRecoveries(rArray: CBORObject?): List<Recovery>? {
        var recList = mutableListOf<Recovery>()
        if (rArray != null && !rArray.isUndefined) {
            for (i in 0 until rArray.size()) {
                val test = rArray[i]
                recList.add(Recovery(
                    parseString(test["tg"]),
                    parseDateType(test["fr"]),
                    parseDateType(test["df"]),
                    parseDateType(test["du"]),
                    parseString(test["co"]),
                    parseString(test["is"]),
                    parseString(test["ci"])
                ))
            }
        }
        return recList
    }

    fun parsePersonName(nam: CBORObject): PersonName? {
        return PersonName(
            parseString(nam["fn"]),
            parseString(nam["fnt"]),
            parseString(nam["gn"]),
            parseString(nam["gnt"])
        )
    }

    fun parseHC1(cert: CBORObject): HC1 {
        return HC1(
            parseString(cert["ver"]),
            parsePersonName(cert["nam"]),
            parseDateType(cert["dob"]),
            parseVaccinations(cert["v"]),
            parseTests(cert["t"]),
            parseRecoveries(cert["r"])
        )
    }

    fun parseCWT(cwt: CBORObject): CWT {
        return CWT(
            parseString(cwt[CWT_ISSUER]),
            parseString(cwt[CWT_SUBJECT]),
            parseString(cwt[CWT_AUDIENCE]),
            parseDateTimeType(cwt[CWT_EXPIRATION]),
            parseDateTimeType(cwt[CWT_NOT_BEFORE]),
            parseDateTimeType(cwt[CWT_ISSUED_AT]),
            parseString(cwt[CWT_ID]),
            parseHC1(cwt[HCERT][HCERT_V1])
        )
    }
}