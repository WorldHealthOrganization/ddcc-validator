package org.who.ddccverifier.map.hcert.dcc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.map.BaseMapping
import org.who.ddccverifier.verify.hcert.dcc.logical.*

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class DCC2FHIR: BaseMapping() {
    fun run(cwt: CWT): Bundle {
        return super.run(
            cwt,
            javaClass.getResourceAsStream("EUDCCtoDDCC.map")
        )
    }
}