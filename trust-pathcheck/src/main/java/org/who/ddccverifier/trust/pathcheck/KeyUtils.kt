package org.who.ddccverifier.trust.pathcheck

import android.util.Base64
import io.ipfs.multibase.Base58
import org.bouncycastle.jcajce.util.BCJcaJceHelper
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.*
import java.security.interfaces.ECPublicKey
import java.security.spec.*
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.cert.Certificate
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey

/**
 * Converts Key formats into Key objects
 */
object KeyUtils {
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

    fun ecPublicKeyFromCoordinate(x: ByteArray, y: ByteArray): PublicKey {
        val ecPoint = ECPoint(BigInteger(1, x), BigInteger(1, y))
        val ecKeySpec = ECPublicKeySpec(ecPoint, ecParameterSpec)
        return KeyFactory.getInstance("EC").generatePublic(ecKeySpec)
    }

    fun ecPublicKeyFromCoordinate(x: String, y: String): PublicKey {
        return ecPublicKeyFromCoordinate(Base64.decode(x, Base64.URL_SAFE),Base64.decode(y, Base64.URL_SAFE))
    }

    fun loadKeySpecFromPEM(pem: String): X509EncodedKeySpec {
        return X509EncodedKeySpec(PemReader(StringReader(pem)).readPemObject().content)
    }

    fun ecPublicKeyFromPEM(pem: String): PublicKey {
        return BCJcaJceHelper().createKeyFactory("EC").generatePublic(loadKeySpecFromPEM(pem))
    }

    fun edPublicKeyFromPEM(pem: String): PublicKey {
        return BCJcaJceHelper().createKeyFactory("Ed25519").generatePublic(loadKeySpecFromPEM(pem))
    }

    fun rsaPublicKeyFromPEM(pem: String): PublicKey {
        return BCJcaJceHelper().createKeyFactory("RSA").generatePublic(loadKeySpecFromPEM(pem))
    }

    fun certificateFromPEM(pem: String): Certificate {
        val reader = PemReader(StringReader(pem))
        val stream = ByteArrayInputStream(reader.readPemObject().content)
        return BCJcaJceHelper().createCertificateFactory("X.509").generateCertificate(stream)
    }

    fun certificatePublicKeyFromPEM(pem: String): PublicKey {
        return certificateFromPEM(pem).publicKey
    }

    fun publicKeyFromPEM(pem: String): PublicKey{
        if (pem.startsWith("-----BEGIN CERTIFICATE-----"))
            return certificatePublicKeyFromPEM(pem)

        //TODO: Figure out a way to get the OID from the PEM bytes to set the right params
        val pk= loadPemOrNull(pem, this::ecPublicKeyFromPEM)
             ?: loadPemOrNull(pem, this::rsaPublicKeyFromPEM)
             ?: loadPemOrNull(pem, this::edPublicKeyFromPEM)

        if (pk != null) {
            return pk
        }

        throw InvalidKeyException()
    }

    private fun loadPemOrNull(pem: String, loader: (String) -> PublicKey): PublicKey? {
        return try {
            loader(pem)
        } catch (e: InvalidKeyException) {
            null
        } catch (e: InvalidKeySpecException) {
            null
        }
    }

    /**
     * Creates a {@link java.security.PublicKey} from an RSA modulus n exponent e
     */
    fun rsaPublicKeyFromModulusExponent(n: ByteArray, e: ByteArray): PublicKey {
        val rsaPublicKeySpec = RSAPublicKeySpec(BigInteger(1, n), BigInteger(1, e))
        return BCJcaJceHelper().createKeyFactory("RSA").generatePublic(rsaPublicKeySpec)
    }

    fun rsaPublicKeyFromModulusExponent(n: String, e: String): PublicKey {
        return rsaPublicKeyFromModulusExponent(Base64.decode(n, Base64.URL_SAFE),Base64.decode(e, Base64.URL_SAFE))
    }

    fun eddsaFromBase58(base58: String): PublicKey {
        val publicKeyBytes = Base58.decode(base58)
        val pubKeyInfo = SubjectPublicKeyInfo(AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), publicKeyBytes)
        val x509KeySpec = X509EncodedKeySpec(pubKeyInfo.encoded)
        return BCJcaJceHelper().createKeyFactory("Ed25519").generatePublic(x509KeySpec) as BCEdDSAPublicKey
    }
}