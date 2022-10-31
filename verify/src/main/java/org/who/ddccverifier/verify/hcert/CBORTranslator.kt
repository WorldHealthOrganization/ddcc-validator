package org.who.ddccverifier.verify.hcert

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.upokecenter.cbor.CBORObject
import org.hl7.fhir.r4.model.Bundle
import org.who.ddccverifier.map.hcert.dcc.DCC2FHIR
import org.who.ddccverifier.verify.hcert.dcc.logical.WHOLogicalModel

/**
 * Finds the right translator for the CBOR content using HC1.
 */
class CBORTranslator {
    val EU_DCC_CODE = -260

    fun toFhir(hcertPayload: CBORObject): Bundle {
        if (hcertPayload[EU_DCC_CODE] != null)
            return DCC2FHIR().run(
                jacksonObjectMapper().readValue(
                    hcertPayload.ToJSONString(),
                    CWT::class.java
                )
            )

        return WHO2FHIR().run(hcertPayload)
    }
}