package org.who.ddccverifier.verify.divoc.jsonldcrypto

import com.danubetech.keyformats.crypto.ByteVerifier
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSVerifier

import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.adapter.JWSVerifierAdapter
import info.weboftrust.ldsignatures.suites.Ed25519Signature2018SignatureSuite
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.util.JWSUtil

import info.weboftrust.ldsignatures.verifier.LdVerifier
import java.security.GeneralSecurityException
import java.security.PublicKey
import java.text.ParseException

class Ed25519Signature2018Verifier
    constructor(verifier: ByteVerifier) : LdVerifier<Ed25519Signature2018SignatureSuite>(SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018, verifier, AndroidURDNA2015Canonicalizer()) {
    constructor(publicKey: PublicKey) : this(Ed25519BouncyCastlePublicKeyVerifier(publicKey))

    override fun verify(signingInput: ByteArray, ldProof: LdProof): Boolean {
        val jws = ldProof.jws ?: throw GeneralSecurityException("No 'jws' in proof.")

        return try {
            val detachedJwsObject = JWSObject.parse(jws)
            val jwsSigningInput = JWSUtil.getJwsSigningInput(detachedJwsObject.header, signingInput)
            val jwsVerifier: JWSVerifier = JWSVerifierAdapter(verifier, JWSAlgorithm.EdDSA)
            jwsVerifier.verify(detachedJwsObject.header, jwsSigningInput, detachedJwsObject.signature)
        } catch (ex: JOSEException) {
            throw GeneralSecurityException("JOSE verification problem: " + ex.message, ex)
        } catch (ex: ParseException) {
            throw GeneralSecurityException("JOSE verification problem: " + ex.message, ex)
        }
    }
}