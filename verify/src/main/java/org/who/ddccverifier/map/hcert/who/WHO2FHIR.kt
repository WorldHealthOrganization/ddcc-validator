package org.who.ddccverifier.map.hcert.who

import org.hl7.fhir.r4.model.Bundle
import org.who.ddccverifier.map.BaseMapper
import org.who.ddccverifier.verify.hcert.dcc.logical.CWT
import org.who.ddccverifier.verify.hcert.dcc.logical.WHOLogicalModel

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class WHO2FHIR: BaseMapper() {
    fun run(who: WHOLogicalModel): Bundle {
        return super.run(
            who,
            javaClass.getResourceAsStream("WHOtoDDCC.map")
        )
    }
}