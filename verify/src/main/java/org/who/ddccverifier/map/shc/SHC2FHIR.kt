package org.who.ddccverifier.map.shc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.map.BaseMapper
import org.who.ddccverifier.verify.shc.JWTPayload

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class SHC2FHIR: BaseMapper() {
    fun run(payload: JWTPayload): Bundle {
        return super.run(
            payload,
            javaClass.getResourceAsStream("SHCtoDDCC.map")
        )
    }
}