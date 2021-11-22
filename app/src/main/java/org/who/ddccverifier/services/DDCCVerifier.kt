package org.who.ddccverifier.services

import nl.minvws.encoding.Base45
import java.util.zip.InflaterInputStream

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import android.util.Base64
import com.upokecenter.cbor.CBORObject
import java.security.PublicKey

class DDCCVerifier {
    private val prefix = "HC1:"

    private fun prefixDecode(qr: String): String {
        return when {
            qr.startsWith(prefix) -> qr.drop(prefix.length)
            else -> qr
        }
    }

    private fun base45Decode(base45: String): ByteArray? {
        try {
            return Base45.getDecoder().decode(base45)
        } catch (e: Throwable) {
            return null
        }
    }

    private fun deflate(input: ByteArray): ByteArray? {
        try {
            return InflaterInputStream(input.inputStream()).readBytes()
        } catch (e: Throwable) {
            return null
        }
    }

    private fun decodeSignedMessage(input: ByteArray): Sign1Message? {
        try {
            return Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
        } catch (e: Throwable) {
            return null
        }
    }

    private fun getKID(input: Sign1Message): String? {
        val kid = input.protectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
               ?: input.unprotectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
               ?: return null
        return Base64.encodeToString(kid, Base64.NO_WRAP)
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return TrustRegistry.resolve(kid)
    }

    private fun getContent(signedMessage: Sign1Message): CBORObject {
        return CBORObject.DecodeFromBytes(signedMessage.GetContent())
    }

    private fun verify(signedMessage: Sign1Message, pubKey: PublicKey): Boolean {
        try {
            val key = OneKey(pubKey, null)
            return signedMessage.validate(key)
        } catch (e: Throwable) {
            return false
        }
    }

    enum class Status {
        INVALID_BASE45,
        INVALID_ZIP,
        INVALID_COSE,
        KID_NOT_INCLUDED,
        ISSUER_NOT_TRUSTED,
        TERMINATED_KEYS,
        EXPIRED_KEYS,
        REVOKED_KEYS,
        NOT_VERIFIED,
        VERIFIED,
    }

    data class VerificationResult (
        var status: Status?,
        var contents: CBORObject?,
        var issuer: TrustRegistry.TrustedEntity?,
        var qr: String,
    )

    fun unpackAndVerify(qr: String): VerificationResult {
        val hc1Decoded = prefixDecode(qr)
        val decodedBytes = base45Decode(hc1Decoded) ?: return VerificationResult(Status.INVALID_BASE45, null, null, qr)
        val deflatedBytes = deflate(decodedBytes) ?: return VerificationResult(Status.INVALID_ZIP, null, null, qr)
        val signedMessage = decodeSignedMessage(deflatedBytes) ?: return VerificationResult(Status.INVALID_COSE, null, null, qr)

        val contents = getContent(signedMessage)

        val kid = getKID(signedMessage) ?: return VerificationResult(Status.KID_NOT_INCLUDED, contents, null, qr)
        val issuer = resolveIssuer(kid) ?: return VerificationResult(Status.ISSUER_NOT_TRUSTED, contents, null, qr)

        when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> return VerificationResult(Status.TERMINATED_KEYS, contents, issuer, qr)
            TrustRegistry.Status.EXPIRED -> return VerificationResult(Status.EXPIRED_KEYS, contents, issuer, qr)
            TrustRegistry.Status.REVOKED -> return VerificationResult(Status.REVOKED_KEYS, contents, issuer, qr)
        }

        if (verify(signedMessage, issuer.pubKey)) {
            return VerificationResult(Status.VERIFIED, contents, issuer, qr)
        }

        return VerificationResult(Status.NOT_VERIFIED, contents, issuer, qr)
    }
}