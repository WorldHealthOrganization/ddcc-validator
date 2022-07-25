package org.who.ddccverifier.verify.shc

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jwt.SignedJWT
import org.hl7.fhir.r4.model.Bundle
import org.who.ddccverifier.trust.TrustRegistry
import java.io.ByteArrayOutputStream
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.util.zip.Inflater

import org.who.ddccverifier.verify.QRDecoder
import java.util.*

class SHCVerifier (private val registry: TrustRegistry) {
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
        val type: List<String>?,
        val credentialSubject: CredentialSubject?,
    )

    data class CredentialSubject(
        val fhirVersion: String?,
        @JsonDeserialize(using = FHIRDeserializer::class)
        @JsonSerialize(using = FHIRSeserializer::class)
        val fhirBundle: Bundle?,
    )

    object FHIRDeserializer : JsonDeserializer<Bundle>() {
        val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Bundle {
            val node = p.readValueAsTree<JsonNode>()
            return fhirContext.newJsonParser().parseResource(node.toPrettyString()) as Bundle
        }
    }

    object FHIRSeserializer : JsonSerializer<Bundle>() {
        val fhirContext = FhirContext.forCached(FhirVersionEnum.R4)

        override fun serialize(value: Bundle?, gen: JsonGenerator?, serializers: SerializerProvider?) {
            if (value != null) {
                val str = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(value)
                gen?.writeRaw(":" + str)
            }
        }
    }

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
                Base64.getUrlDecoder().decode(parts[0]),
                Base64.getUrlDecoder().decode(parts[1]),
                Base64.getUrlDecoder().decode(parts[2])
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
            e.printStackTrace()
            null
        }
    }

    private fun getKID(jwt: JWT): String? {
        if (jwt.payload.iss == null || jwt.header.kid == null)
            return null

        return "${jwt.payload.iss}#${jwt.header.kid}"
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return registry.resolve(TrustRegistry.Framework.SHC, kid)
    }

    private fun verify(signedMessage: SignedJWT, pubKey: PublicKey): Boolean {
        return try {
            val key = ECDSAVerifier(pubKey as ECPublicKey)
            signedMessage.verify(key)
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun unpackAndVerify(uri: String): QRDecoder.VerificationResult {
        val hc1Decoded = prefixDecode(uri)
        val decodedBytes = fromBase10(hc1Decoded) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, uri, null)
        val jwtRaw = parseJWT(decodedBytes) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, uri, null)
        val jwt = parsePayload(jwtRaw) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_COMPRESSION, null, null, uri, null)

        val unpacked = jacksonObjectMapper().writeValueAsString(jwt)

        val signedMessage = decodeSignedMessage(decodedBytes) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, uri, unpacked)

        val contents = JWTTranslator().toFhir(jwt.payload)

        val kid = getKID(jwt) ?: return QRDecoder.VerificationResult(QRDecoder.Status.KID_NOT_INCLUDED, contents, null, uri, unpacked)
        val issuer = resolveIssuer(kid) ?: return QRDecoder.VerificationResult(QRDecoder.Status.ISSUER_NOT_TRUSTED, contents, null, uri, unpacked)

        return when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> QRDecoder.VerificationResult(QRDecoder.Status.TERMINATED_KEYS, contents, issuer, uri, unpacked)
            TrustRegistry.Status.EXPIRED -> QRDecoder.VerificationResult(QRDecoder.Status.EXPIRED_KEYS, contents, issuer, uri, unpacked)
            TrustRegistry.Status.REVOKED -> QRDecoder.VerificationResult(QRDecoder.Status.REVOKED_KEYS, contents, issuer, uri, unpacked)
            TrustRegistry.Status.CURRENT ->
                if (verify(signedMessage, issuer.publicKey))
                    QRDecoder.VerificationResult(QRDecoder.Status.VERIFIED, contents, issuer, uri, unpacked)
                else
                    QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNATURE, contents, issuer, uri, unpacked)
        }
    }
}