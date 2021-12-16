package org.who.ddccverifier.services.qrs.hcert

import com.upokecenter.cbor.CBORObject
import org.hl7.fhir.r4.model.Composition

/**
 * Finds the right translator for the CBOR content using HC1.
 */
class CBORTranslator {
    fun toFhir(hcertPayload: CBORObject): Composition {
        return WHOCBOR2FHIR().run(hcertPayload)
    }
}