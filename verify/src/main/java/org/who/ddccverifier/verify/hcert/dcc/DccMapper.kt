package org.who.ddccverifier.verify.hcert.dcc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.verify.BaseMapper
import org.who.ddccverifier.verify.hcert.dcc.logical.*

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class DccMapper: BaseMapper() {
    fun run(cwt: CWT): Bundle {
        return super.run(
            cwt,
            javaClass.getResourceAsStream("EUDCCtoDDCC.map")
        )
    }
}