package org.who.ddccverifier.services.qrs.hcert

import nl.minvws.encoding.Base45
import java.util.zip.InflaterInputStream

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import android.util.Base64
import com.upokecenter.cbor.CBORObject
import org.who.ddccverifier.services.qrs.QRUnpacker
import org.who.ddccverifier.services.trust.TrustRegistry
import java.security.PublicKey

/**
 * Turns HC1 QR Codes into CBOR Objects
 */
class HCERTVerifier {
    private val prefix = "HC1:"

    private fun prefixDecode(qr: String): String {
        return when {
            qr.startsWith(prefix) -> qr.drop(prefix.length)
            else -> qr
        }
    }

    private fun base45Decode(base45: String): ByteArray? {
        return try {
            Base45.getDecoder().decode(base45)
        } catch (e: Throwable) {
            null
        }
    }

    private fun deflate(input: ByteArray): ByteArray? {
        return try {
            InflaterInputStream(input.inputStream()).readBytes()
        } catch (e: Throwable) {
            null
        }
    }

    private fun decodeSignedMessage(input: ByteArray): Sign1Message? {
        return try {
            Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
        } catch (e: Throwable) {
            null
        }
    }

    private fun getKID(input: Sign1Message): String? {
        val kid = input.protectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
               ?: input.unprotectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
               ?: return null
        return Base64.encodeToString(kid, Base64.NO_WRAP)
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return TrustRegistry.resolve(TrustRegistry.Framework.DCC, kid)
    }

    private fun getContent(signedMessage: Sign1Message): CBORObject {
        return CBORObject.DecodeFromBytes(signedMessage.GetContent())
    }

    private fun verify(signedMessage: Sign1Message, pubKey: PublicKey): Boolean {
        return try {
            val key = OneKey(pubKey, null)
            signedMessage.validate(key)
        } catch (e: Throwable) {
            false
        }
    }

    fun unpack(qr: String): CBORObject? {
        val hc1Decoded = prefixDecode(qr)
        val decodedBytes = base45Decode(hc1Decoded) ?: return null
        val deflatedBytes = deflate(decodedBytes) ?: return null
        val signedMessage = decodeSignedMessage(deflatedBytes) ?: return null
        return getContent(signedMessage);
    }

    fun unpackAndVerify(qr: String): QRUnpacker.VerificationResult {
        val hc1Decoded = prefixDecode(qr)
        val decodedBytes = base45Decode(hc1Decoded) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_BASE45, null, null, qr)
        val deflatedBytes = deflate(decodedBytes) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_ZIP, null, null, qr)
        val signedMessage = decodeSignedMessage(deflatedBytes) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_COSE, null, null, qr)

        val contents = CBORTranslator().toFhir(getContent(signedMessage))

        val kid = getKID(signedMessage) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.KID_NOT_INCLUDED, contents, null, qr)
        val issuer = resolveIssuer(kid) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.ISSUER_NOT_TRUSTED, contents, null, qr)

        when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> return QRUnpacker.VerificationResult(QRUnpacker.Status.TERMINATED_KEYS, contents, issuer, qr)
            TrustRegistry.Status.EXPIRED -> return QRUnpacker.VerificationResult(QRUnpacker.Status.EXPIRED_KEYS, contents, issuer, qr)
            TrustRegistry.Status.REVOKED -> return QRUnpacker.VerificationResult(QRUnpacker.Status.REVOKED_KEYS, contents, issuer, qr)
        }

        if (verify(signedMessage, issuer.didDocument)) {
            return QRUnpacker.VerificationResult(QRUnpacker.Status.VERIFIED, contents, issuer, qr)
        }

        return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_SIGNATURE, contents, issuer, qr)
    }
}