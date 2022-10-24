package org.who.ddccverifier.verify.hcert

import com.upokecenter.cbor.CBORObject
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.who.ddccverifier.verify.hcert.dcc.CBORParser
import org.who.ddccverifier.map.hcert.dcc.DCC2FHIR

/**
 * Finds the right translator for the CBOR content using HC1.
 */
class CBORTranslator {
    val EU_DCC_CODE = -260

    fun toFhir(hcertPayload: CBORObject): Bundle {
        if (hcertPayload[EU_DCC_CODE] != null)
            return DCC2FHIR().run(CBORParser().parseCWT(hcertPayload))

        return WHO2FHIR().run(hcertPayload)
    }
}