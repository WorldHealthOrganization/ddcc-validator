package org.who.ddccverifier.services

import org.hl7.fhir.r4.model.Composition
import org.who.ddccverifier.services.qrs.divoc.DivocVerifier
import org.who.ddccverifier.services.qrs.hcert.HCertVerifier
import org.who.ddccverifier.services.qrs.icao.IcaoVerifier
import org.who.ddccverifier.services.qrs.shc.SHCVerifier
import org.who.ddccverifier.services.trust.TrustRegistry
import java.io.InputStream

/**
 * Finds the right processor for the QR Content and returns the DDCC Composition of that Content.
 */
class QRDecoder(private val open: (String)-> InputStream?) {
    enum class Status {
        NOT_SUPPORTED,
        INVALID_BASE45,
        INVALID_ZIP,
        INVALID_COSE,
        KID_NOT_INCLUDED,
        ISSUER_NOT_TRUSTED,
        TERMINATED_KEYS,
        EXPIRED_KEYS,
        REVOKED_KEYS,
        INVALID_SIGNATURE,
        VERIFIED,
    }

    data class VerificationResult (
        var status: Status,
        var contents: Composition?, // the DDCC Composition
        var issuer: TrustRegistry.TrustedEntity?,
        var qr: String
    )

    fun decode(qrPayload : String): VerificationResult {
        if (qrPayload.uppercase().startsWith("HC1:")) {
            return HCertVerifier().unpackAndVerify(qrPayload)
        }
        if (qrPayload.uppercase().startsWith("SHC:")) {
            return SHCVerifier().unpackAndVerify(qrPayload)
        }
        if (qrPayload.uppercase().startsWith("B64:") || qrPayload.uppercase().startsWith("PK")) {
            return DivocVerifier(open).unpackAndVerify(qrPayload)
        }
        if (qrPayload.uppercase().contains("ICAO")) {
            return IcaoVerifier().unpackAndVerify(qrPayload)
        }

        return VerificationResult(Status.NOT_SUPPORTED, null, null, qrPayload)
    }
}