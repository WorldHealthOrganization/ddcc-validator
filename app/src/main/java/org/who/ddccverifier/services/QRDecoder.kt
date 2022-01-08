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
        NOT_SUPPORTED, // QR Standard not supported by this algorithm
        INVALID_ENCODING, // could not decode Base45 for DCC, Base10 for SHC,
        INVALID_COMPRESSION, // could not decompress the byte array
        INVALID_SIGNING_FORMAT, // invalid COSE, JOSE, W3C VC Payload
        KID_NOT_INCLUDED, // unable to resolve the issuer ID
        ISSUER_NOT_TRUSTED, // issuer is not found in the registry
        TERMINATED_KEYS, // issuer was terminated by the registry
        EXPIRED_KEYS, // keys expired
        REVOKED_KEYS, // keys were revoked by the issuer
        INVALID_SIGNATURE, // signature doesn't match
        VERIFIED,  // Verified content.
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