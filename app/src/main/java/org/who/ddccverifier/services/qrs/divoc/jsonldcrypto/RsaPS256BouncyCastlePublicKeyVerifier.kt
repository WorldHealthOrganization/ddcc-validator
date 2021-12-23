package org.who.ddccverifier.services.qrs.divoc.jsonldcrypto

import com.danubetech.keyformats.crypto.PublicKeyVerifier
import com.danubetech.keyformats.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import java.security.PublicKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec

/**
 * Override com.danubetech.keyformats.crypto.impl.RSA_PS256_PublicKeyVerifier to force
 * the use of BouncyCastle's provider
 */
class RsaPS256BouncyCastlePublicKeyVerifier(publicKey: PublicKey) :
    PublicKeyVerifier<PublicKey>(publicKey, JWSAlgorithm.PS256) {

    public override fun verify(content: ByteArray, signature: ByteArray): Boolean {
        val pssParameterSpec = PSSParameterSpec("SHA256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1)
        val jcaSignature = Signature.getInstance("SHA256withRSAandMGF1", BouncyCastleProviderSingleton.getInstance())
        jcaSignature.setParameter(pssParameterSpec)
        jcaSignature.initVerify(publicKey)
        jcaSignature.update(content)
        return jcaSignature.verify(signature)
    }
}