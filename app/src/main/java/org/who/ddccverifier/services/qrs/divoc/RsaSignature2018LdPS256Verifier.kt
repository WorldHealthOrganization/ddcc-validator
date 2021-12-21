package org.who.ddccverifier.services.qrs.divoc

import com.danubetech.keyformats.crypto.ByteVerifier
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PublicKeyVerifier
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSVerifier
import foundation.identity.jsonld.JsonLDException
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.adapter.JWSVerifierAdapter
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizer
import info.weboftrust.ldsignatures.canonicalizer.URDNA2015Canonicalizer
import info.weboftrust.ldsignatures.suites.RsaSignature2018SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.util.JWSUtil
import info.weboftrust.ldsignatures.util.SHAUtil
import info.weboftrust.ldsignatures.verifier.LdVerifier
import java.io.IOException
import java.net.URI
import java.security.GeneralSecurityException
import java.security.PublicKey
import java.text.ParseException
import java.util.List

class RsaSignature2018LdPS256Verifier
    @JvmOverloads constructor(verifier: ByteVerifier?) :
    LdVerifier<RsaSignature2018SignatureSuite?>(SignatureSuites.SIGNATURE_SUITE_RSASIGNATURE2018, verifier, MyURDNA2015Canonicalizer()) {

    constructor(publicKey: PublicKey?) : this(RSA_PS256_PublicKeyVerifier(publicKey as java.security.interfaces.RSAPublicKey)) {}

    override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
        try {
            val detachedJwsObject = JWSObject.parse(ldProof.jws)

            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)

            val jwsVerifier: JWSVerifier = JWSVerifierAdapter(verifier, JWSAlgorithm.PS256)

            return jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        } catch (ex: JOSEException) {
            throw GeneralSecurityException("JOSE verification problem: " + ex.message, ex)
        } catch (ex: ParseException) {
            throw GeneralSecurityException("JOSE verification problem: " + ex.message, ex)
        }
    }

    class MyURDNA2015Canonicalizer : Canonicalizer(listOf("urdna2015")) {
        override fun canonicalize(ldProof: LdProof, jsonLdObject: JsonLDObject): ByteArray {
            // construct the LD proof without proof values
            val ldProofWithoutProofValues = LdProof.builder()
                .base(ldProof)
                .defaultContexts(true)
                .build()
            ldProofWithoutProofValues.documentLoader = jsonLdObject.documentLoader
            LdProof.removeLdProofValues(ldProofWithoutProofValues)

            // construct the LD object without proof
            val jsonLdObjectWithoutProof = JsonLDObject.builder()
                .base(jsonLdObject)
                .build()
            jsonLdObjectWithoutProof.documentLoader = jsonLdObject.documentLoader
            LdProof.removeFromJsonLdObject(jsonLdObjectWithoutProof)

            // canonicalize the LD proof and LD object
            val canonicalizedLdProofWithoutProofValues = ldProofWithoutProofValues.normalize("urdna2015")
            val canonicalizedJsonLdObjectWithoutProof = jsonLdObjectWithoutProof.normalize("urdna2015")

            // construct the canonicalization result
            val canonicalizationResult = ByteArray(64)
            System.arraycopy(SHAUtil.sha256(canonicalizedLdProofWithoutProofValues), 0, canonicalizationResult, 0, 32)
            System.arraycopy(SHAUtil.sha256(canonicalizedJsonLdObjectWithoutProof), 0, canonicalizationResult, 32,32)
            return canonicalizationResult
        }
    }
}