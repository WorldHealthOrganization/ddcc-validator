package org.who.ddccverifier.verify.hcert.dcc

import org.hl7.fhir.r4.model.Bundle
import org.who.ddccverifier.verify.BaseMapper
import org.who.ddccverifier.verify.hcert.dcc.logical.CWT

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class DccMapper: BaseMapper() {
    fun run(cwt: CWT): Bundle {
        return super.run(
            cwt,
            "EUDCCtoDDCC.map"
        )
    }
}