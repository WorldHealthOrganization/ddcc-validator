package org.who.ddccverifier.services.qrs.divoc.jsonldcrypto

import com.danubetech.keyformats.crypto.PublicKeyVerifier
import com.danubetech.keyformats.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import java.security.PublicKey
import java.security.Signature

/**
 * Override com.danubetech.keyformats.crypto.impl.Ed25519_PublicKeyVerifier to force
 * the use of BouncyCastle's provider
 */
class Ed25519BouncyCastlePublicKeyVerifier(publicKey: PublicKey) :
    PublicKeyVerifier<PublicKey>(publicKey, JWSAlgorithm.EdDSA) {

    public override fun verify(content: ByteArray, signature: ByteArray): Boolean {
        val jcaSignature = Signature.getInstance("Ed25519", BouncyCastleProviderSingleton.getInstance())
        jcaSignature.initVerify(this.publicKey)
        jcaSignature.update(content)
        return jcaSignature.verify(signature)
    }
}