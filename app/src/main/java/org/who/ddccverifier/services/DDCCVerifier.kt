package org.who.ddccverifier.services

import nl.minvws.encoding.Base45
import java.util.zip.InflaterInputStream

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import android.os.Build
import androidx.annotation.RequiresApi
import com.upokecenter.cbor.CBORObject
import java.time.Instant
import java.util.*

class DDCCVerifier {
    private val HC1 = "HC1:"

    private fun prefixDecode(qr: String): String {
        return when {
            qr.startsWith(HC1) -> qr.drop(HC1.length)
            else -> qr
        };
    }

    private fun base45Decode(base45: String): ByteArray {
        return Base45.getDecoder().decode(base45);
    }

    private fun deflate(input: ByteArray): ByteArray {
        return InflaterInputStream(input.inputStream()).readBytes()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun verify(input: ByteArray): ByteArray? {
        return try {
            val signature: Sign1Message = Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
            println(signature.protectedAttributes);
            println(signature.unprotectedAttributes);

            val kid: ByteArray = signature.protectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
                              ?: signature.unprotectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
                              ?: return null // TODO: Error message: Signer has not been declared.

            val issuer: TrustRegistry.TrustedEntity = TrustRegistry.resolve(Base64.getEncoder().encodeToString(kid))
                ?: return null // TODO: Error message: Trust Registry doesn't know the signer.

            val pubKey = OneKey(issuer.pubKey, null)
            signature.validate(pubKey)

            when (issuer.status) {
                TrustRegistry.Status.CURRENT -> return signature.GetContent()
                // TODO: Error message: Issuer has broken the trust. Key was terminated by the registry.
                TrustRegistry.Status.TERMINATED -> return null;
                // TODO: Error message: Key is expired. Please get a new Credential from the issuer.
                TrustRegistry.Status.EXPIRED -> return null;
                // TODO: Error message: Key is has leaked. Issuer has revoked the keys.
                TrustRegistry.Status.REVOKED -> return null;
                // TODO: Error message: Unkown Status
                else -> return null;
            }
        } catch (e: Throwable) {
            e.printStackTrace();
            null
        }
    }

    private fun cborDecode(input: ByteArray): CBORObject {
        return CBORObject.DecodeFromBytes(input);
    }

    fun unpackAndVerify(qr: String): CBORObject? {
        println("QR: " + qr);
        val hc1Decoded: String = prefixDecode(qr);
        val decodedBytes: ByteArray = base45Decode(hc1Decoded)
        val deflatedBytes: ByteArray = deflate(decodedBytes)
        val verified: ByteArray? = verify(deflatedBytes);

        if (verified == null) return null

        val cborDecoded: CBORObject = cborDecode(verified!!);
        println("CBOR Decoded: " + cborDecoded);
        return cborDecoded
    }
}