package org.who.ddccverifier.services.qrs.shc

import android.util.Base64
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.hl7.fhir.r4.model.Bundle
import org.who.ddccverifier.services.trust.TrustRegistry
import java.io.ByteArrayOutputStream
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.util.zip.Inflater

import org.who.ddccverifier.services.qrs.QRUnpacker


class SHCVerifier {
    private val URI_SCHEMA = "shc"
    private val SMALLEST_B64_CHAR_CODE = 45

    data class JWTHeader(
        val alg: String?,
        val kid: String?,
        val zip: String?,
    )

    data class JWTPayload(
        val iss: String?,
        val sub: String?,
        val aud: String?,
        val exp: Double?, // some idiots use floating point
        val nbf: Double?, // some idiots use floating point
        val iat: Double?, // some idiots use floating point
        val jti: String?,
        val vc: VC?,
    )

    data class VC(
        val type: Array<String>?,
        val credentialSubject: CredentialSubject?,
    )

    object FHIRDeserializer : JsonDeserializer<Bundle>() {
        val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Bundle {
            val node = p.readValueAsTree<JsonNode>()
            return fhirContext.newJsonParser().parseResource(node.toPrettyString()) as Bundle
        }
    }

    data class CredentialSubject(
        val fhirVersion: String?,
        @JsonDeserialize(using = FHIRDeserializer::class)
        val fhirBundle: Bundle?,
    )

    fun inflateRaw(byteArray: ByteArray): ByteArray {
        val decompresser = Inflater(true)
        decompresser.setInput(byteArray)

        val outputStream = ByteArrayOutputStream(byteArray.size)
        val buffer = ByteArray(1024)
        while (!decompresser.finished()) {
            val count: Int = decompresser.inflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        outputStream.close()
        return outputStream.toByteArray()
    }

    data class JWTRaw(
        val header: ByteArray,
        val payload: ByteArray,
        val verified: ByteArray,
    )

    private fun parseJWT(token: String): JWTRaw? {
        return try {
            val parts = token.split(".") // always returns 3 parts

            JWTRaw(
                Base64.decode(parts[0], Base64.URL_SAFE),
                Base64.decode(parts[1], Base64.URL_SAFE),
                Base64.decode(parts[2], Base64.URL_SAFE)
            )
        } catch (e: Throwable) {
            return null
        }
    }

    private fun parsePayload(jwt: JWTRaw): JWT? {
        return try {
            val mapper = jacksonObjectMapper()
            val header = mapper.readValue(jwt.header.toString(Charsets.UTF_8), JWTHeader::class.java)
            var payloadRaw = jwt.payload

            if (header.zip.equals("DEF")) {
                payloadRaw = inflateRaw(jwt.payload)
            }

            val payload = mapper.readValue(payloadRaw, JWTPayload::class.java)

            JWT(header, payload)
        } catch (e: Throwable) {
            return null
        }
    }

    data class JWT(
        val header: JWTHeader,
        val payload: JWTPayload
    )

    private fun fromBase10(base10: String): String? {
        return try {
            return base10.chunked(2)
                .map { Char(it.toInt() + SMALLEST_B64_CHAR_CODE) }
                .joinToString("")
        } catch (e: Throwable) {
            null
        }
    }

    fun unpack(uri: String): String? {
        return fromBase10(prefixDecode(uri))
    }

    private fun prefixDecode(uri: String): String {
        var data = uri.lowercase()

        // Backwards compatibility.
        if (data.startsWith(URI_SCHEMA)) {
            data = data.substring(3)
            if (data.startsWith(':')) {
                data = data.substring(1)
            }
            if (data.startsWith('/')) {
                data = data.substring(1)
            }
        }

        return data
    }

    private fun decodeSignedMessage(decodedBytes: String): SignedJWT? {
        return try {
            return SignedJWT.parse(decodedBytes)
        } catch (e: Throwable) {
            null
        }
    }

    private fun getKID(jwt: JWT): String? {
        if (jwt.payload.iss == null || jwt.header.kid == null)
            return null

        return "${jwt.payload.iss}#${jwt.header.kid}"
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return TrustRegistry.resolve(TrustRegistry.Framework.SHC, kid)
    }

    private fun verify(signedMessage: SignedJWT, pubKey: PublicKey): Boolean {
        return try {
            val key = ECDSAVerifier(pubKey as ECPublicKey)
            signedMessage.verify(key)
        } catch (e: Throwable) {
            false
        }
    }

    fun unpackAndVerify(uri: String): QRUnpacker.VerificationResult {
        val hc1Decoded = prefixDecode(uri)
        val decodedBytes = fromBase10(hc1Decoded) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_BASE45, null, null, uri)
        val jwtRaw = parseJWT(decodedBytes) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_BASE45, null, null, uri)
        val jwt = parsePayload(jwtRaw) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_ZIP, null, null, uri)
        val signedMessage = decodeSignedMessage(decodedBytes) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_COSE, null, null, uri)

        val contents = JWTTranslator().toFhir(jwt.payload)

        val kid = getKID(jwt) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.KID_NOT_INCLUDED, contents, null, uri)
        val issuer = resolveIssuer(kid) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.ISSUER_NOT_TRUSTED, contents, null, uri)

        when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> return QRUnpacker.VerificationResult(QRUnpacker.Status.TERMINATED_KEYS, contents, issuer, uri)
            TrustRegistry.Status.EXPIRED -> return QRUnpacker.VerificationResult(QRUnpacker.Status.EXPIRED_KEYS, contents, issuer, uri)
            TrustRegistry.Status.REVOKED -> return QRUnpacker.VerificationResult(QRUnpacker.Status.REVOKED_KEYS, contents, issuer, uri)
        }

        if (verify(signedMessage, issuer.didDocument)) {
            return QRUnpacker.VerificationResult(QRUnpacker.Status.VERIFIED, contents, issuer, uri)
        }

        return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_SIGNATURE, contents, issuer, uri)
    }
}