package org.who.ddccverifier.map.divoc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.map.BaseMapper
import org.who.ddccverifier.verify.divoc.W3CVC

/**
 * Translates a W3C VC object into FHIR Objects
 */
class DIVOC2FHIR: BaseMapper() {
    fun run(payload: W3CVC): Bundle {
        return super.run(
            payload,
            javaClass.getResourceAsStream("DIVOCtoDDCC.map")
        )
    }
}