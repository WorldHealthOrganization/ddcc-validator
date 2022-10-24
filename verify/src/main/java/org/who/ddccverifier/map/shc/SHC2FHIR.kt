package org.who.ddccverifier.map.shc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.map.BaseMapping
import org.who.ddccverifier.verify.hcert.dcc.logical.*
import org.who.ddccverifier.verify.icao.IJson
import org.who.ddccverifier.verify.shc.JWTPayload

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class SHC2FHIR: BaseMapping() {
    fun run(payload: JWTPayload): Bundle {
        return super.run(
            payload,
            javaClass.getResourceAsStream("SHCtoDDCC.map")
        )
    }
}