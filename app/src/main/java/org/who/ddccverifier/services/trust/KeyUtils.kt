package org.who.ddccverifier.services.trust

import android.util.Base64
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.*

/**
 * Converts Key formats into Key objects
 */
class KeyUtils {
    private var ecParameterSpec: ECParameterSpec? = null
        get() {
            if (field == null) {
                val ecParameters = ECGenParameterSpec("secp256r1") //prime256v1
                field = try {
                    val algorithmParameters = AlgorithmParameters.getInstance("EC") // ECDSA
                    algorithmParameters.init(ecParameters)

                    algorithmParameters.getParameterSpec(ECParameterSpec::class.java)
                } catch (e: Exception) {
                    // This is a work-around for getting the ECParameterSpec since EC is only available in AlgorithmParameters on API 26+
                    val kpg = KeyPairGenerator.getInstance("EC")
                    kpg.initialize(ecParameters, SecureRandom())
                    val keyPair = kpg.generateKeyPair()
                    (keyPair.public as ECPublicKey).params
                }
            }
            return field
        }

    /**
     * Creates a {@link java.security.PublicKey} from a coordinate point (x, y).
     * Assumes curve P-256.
     */
    fun ecPublicKeyFromCoordinate(x: ByteArray, y: ByteArray): PublicKey {
        val ecPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))
        val ecKeySpec = ECPublicKeySpec(ecPoint, ecParameterSpec)

        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(ecKeySpec)
    }

    fun ecPublicKeyFromCoordinate(x: String, y: String): PublicKey {
        return ecPublicKeyFromCoordinate(Base64.decode(x, Base64.URL_SAFE),Base64.decode(y, Base64.URL_SAFE))
    }

    fun ecPublicKeyFromPEM(pem: String): PublicKey {
        val publicKeyPEM = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END PUBLIC KEY-----", "")
        val encoded: ByteArray = Base64.decode(publicKeyPEM, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("EC")
        val keySpec = X509EncodedKeySpec(encoded)
        return keyFactory.generatePublic(keySpec)
    }

    fun certificateFromPEM(pem: String): PublicKey {
        val publicKeyPEM = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END CERTIFICATE-----", "")
        val encoded: ByteArray = Base64.decode(publicKeyPEM, Base64.DEFAULT)

        val cf = CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(ByteArrayInputStream(encoded))

        return cert.publicKey;
    }

    fun publicKeyFromPEM(pem: String): PublicKey{
        if (pem.startsWith("-----BEGIN CERTIFICATE-----"))
            return certificateFromPEM(pem)

        //TODO: Figure out a way to get the OID from the PEM bytes to set the right params
        try {
            return ecPublicKeyFromPEM(pem)
        } catch (e: InvalidKeyException) {
            return rsaPublicKeyFromPEM(pem)
        } catch (e: InvalidKeySpecException) {
            return rsaPublicKeyFromPEM(pem)
        }
    }

    /**
     * Creates a {@link java.security.PublicKey} from an RSA modulus n exponent e
     */
    fun rsaPublicKeyFromModulusExponent(n: ByteArray, e: ByteArray): PublicKey {
        val rsaPublicKeySpec = RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(rsaPublicKeySpec)
    }

    fun rsaPublicKeyFromModulusExponent(n: String, e: String): PublicKey {
        return rsaPublicKeyFromModulusExponent(Base64.decode(n, Base64.URL_SAFE),Base64.decode(e, Base64.URL_SAFE))
    }

    fun rsaPublicKeyFromPEM(pem: String): PublicKey {
        val publicKeyPEM = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace(System.lineSeparator().toRegex(), "")
            .replace("-----END PUBLIC KEY-----", "")
        val encoded: ByteArray = Base64.decode(publicKeyPEM, Base64.DEFAULT)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(encoded)
        return keyFactory.generatePublic(keySpec)
    }
}