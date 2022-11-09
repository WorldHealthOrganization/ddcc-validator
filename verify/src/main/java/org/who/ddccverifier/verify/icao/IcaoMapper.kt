package org.who.ddccverifier.verify.icao

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.verify.BaseMapper

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class IcaoMapper: BaseMapper() {
    fun run(iJson: IJson): Bundle {
        return super.run(
            iJson,
            "ICAOtoDDCC.map"
        )
    }
}