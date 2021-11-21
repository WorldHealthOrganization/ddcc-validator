package org.who.ddccverifier.services

import nl.minvws.encoding.Base45
import java.util.zip.InflaterInputStream

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import com.upokecenter.cbor.CBORObject
import java.time.Instant

class DDCCVerifier {
    private val HC1 = "HC1:"

    fun prefixDecode(qr: String): String {
        return when {
            qr.startsWith(HC1) -> qr.drop(HC1.length)
            else -> qr
        };
    }

    fun base45Decode(base45: String): ByteArray {
        return Base45.getDecoder().decode(base45);
    }

    fun deflate(input: ByteArray): ByteArray {
        return InflaterInputStream(input.inputStream()).readBytes()
    }

    fun verify(input: ByteArray): ByteArray? {
        return try {
            val signature: Sign1Message = Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message
            println(signature.protectedAttributes);
            println(signature.unprotectedAttributes);

            val kid: ByteArray = signature.protectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
                              ?: signature.unprotectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
                              ?: return null

            val pk = TrustRegistry().resolve(kid);

            val pubKey = OneKey(pk, null)
            signature.validate(pubKey);
            signature.GetContent()
        } catch (e: Throwable) {
            e.printStackTrace();
            null
        }
    }

    fun cborDecode(input: ByteArray): CBORObject {
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