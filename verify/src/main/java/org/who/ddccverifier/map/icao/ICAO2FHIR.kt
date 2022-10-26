package org.who.ddccverifier.map.icao

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.map.BaseMapper
import org.who.ddccverifier.verify.icao.IJson

/**
 * Translates a DDCC QR CBOR object into FHIR Objects
 */
class ICAO2FHIR: BaseMapper() {
    fun run(iJson: IJson): Bundle {
        return super.run(
            iJson,
            javaClass.getResourceAsStream("ICAOtoDDCC.map")
        )
    }
}