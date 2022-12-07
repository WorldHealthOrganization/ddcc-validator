package org.who.ddccverifier.verify.shc

import org.hl7.fhir.r4.model.Bundle
import org.who.ddccverifier.verify.BaseMapper

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class ShcMapper: BaseMapper() {
    fun run(payload: JWTPayload): Bundle {
        return super.run(
            payload,
            "SHCtoDDCC.map"
        )
    }
}