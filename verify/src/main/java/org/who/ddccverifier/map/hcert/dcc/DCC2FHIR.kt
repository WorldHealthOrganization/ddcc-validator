package org.who.ddccverifier.map.hcert.dcc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.map.BaseMapper
import org.who.ddccverifier.verify.hcert.dcc.logical.*

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class DCC2FHIR: BaseMapper() {
    fun run(cwt: CWT): Bundle {
        return super.run(
            cwt,
            javaClass.getResourceAsStream("EUDCCtoDDCC.map")
        )
    }
}