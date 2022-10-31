package org.who.ddccverifier.verify.divoc

import org.hl7.fhir.r4.model.*
import org.who.ddccverifier.verify.BaseMapper

/**
 * Translates a W3C VC object into FHIR Objects
 */
class DivocMapper: BaseMapper() {
    fun run(payload: W3CVC): Bundle {
        return super.run(
            payload,
            javaClass.getResourceAsStream("DIVOCtoDDCC.map")
        )
    }
}