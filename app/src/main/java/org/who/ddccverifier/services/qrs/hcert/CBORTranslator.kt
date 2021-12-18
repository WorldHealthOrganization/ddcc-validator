package org.who.ddccverifier.services.qrs.hcert

import com.upokecenter.cbor.CBORObject
import org.hl7.fhir.r4.model.Composition

/**
 * Finds the right translator for the CBOR content using HC1.
 */
class CBORTranslator {
    val EU_DCC_CODE = -260

    fun toFhir(hcertPayload: CBORObject): Composition {
        if (hcertPayload[EU_DCC_CODE] != null)
            return DCC2FHIR().run(hcertPayload)

        return WHO2FHIR().run(hcertPayload)
    }
}