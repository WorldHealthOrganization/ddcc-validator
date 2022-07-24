package org.who.ddccverifier.verify.divoc

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import foundation.identity.jsonld.JsonLDObject
import org.who.ddccverifier.trust.TrustRegistry
import java.security.PublicKey

import org.who.ddccverifier.verify.QRDecoder
import org.who.ddccverifier.verify.divoc.jsonldcrypto.RsaSignature2018withPS256Verifier
import org.who.ddccverifier.verify.divoc.jsonldcrypto.Ed25519Signature2018Verifier
import java.io.ByteArrayInputStream
import java.util.*
import java.util.zip.ZipInputStream

class DivocVerifier(private val registry: TrustRegistry) {
    private val URI_SCHEMA = "B64:"

    data class W3CVC(
        @JsonProperty("@context")
        val context: List<String>,
        val type: List<String>,
        val issuer: String,
        val issuanceDate: String,  // Do not convert to Date to avoid creating precision/timezone problems
        val nonTransferable: Boolean?,
        val credentialSubject: CredentialSubject,
        val evidence: List<Evidence>,
        val proof: Proof?,
    )

    data class CredentialSubject(
        val type: String?,
        val id: String?,
        val uhid: String?,
        val refId: String?,
        val name: String?,
        val gender: String?,
        val sex: String?,
        val age: String?, //V1 // Do not convert to Date to avoid creating precision/timezone problems
        val dob: String?, //V2 // Do not convert to Date to avoid creating precision/timezone problems
        val nationality: String?,
        val address: Address?,
    )

    data class Proof(
        val type: String?,
        val created: String?,
        val verificationMethod: String?,
        val proofPurpose: String?,
        val jws: String?,
    )

    data class Address(
        val streetAddress: String?,
        val streetAddress2: String?,
        val district: String?,
        val city: String?,
        val addressRegion: String?,
        val addressCountry: String?,
        val postalCode: String?,
    )

    data class Evidence(
        val id: String?,
        val feedbackUrl: String?,
        val infoUrl: String?,
        val certificateId: String?,
        val type: List<String>?,
        val batch: String?,
        val vaccine: String?,
        val manufacturer: String?,
        val date: String?,           // Do not convert to Date to avoid creating precision/timezone problems
        val effectiveStart: String?, // Do not convert to Date to avoid creating precision/timezone problems
        val effectiveUntil: String?, // Do not convert to Date to avoid creating precision/timezone problems
        val dose: Int?,
        val totalDoses: Int?,
        val verifier: Verifier?,
        val facility: Facility?,
        val icd11Code: String?,  //V2
        val prophylaxis: String?,  //V2
    )

    data class Verifier(
        val name: String?,
    )

    data class Facility(
        val name: String?,
        val address: Address?,
    )

    private fun map(jsonStr: String): W3CVC? {
        return try {
            val mapper = jacksonObjectMapper()
            return mapper.readValue(jsonStr, W3CVC::class.java)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun buildJSonLDDocument(str: String):JsonLDObject? {
        return try {
            val contexts = ContextLoader()
            val jsonLdObject = JsonLDObject.fromJson(str)
            jsonLdObject.documentLoader = contexts
            return jsonLdObject
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun prefixDecode(uri: String): ByteArray? {
        if (uri.uppercase().startsWith(URI_SCHEMA)) {
            return Base64.getDecoder().decode(uri.substring(URI_SCHEMA.length))
        } else if (uri.uppercase().startsWith("PK")) {
            return uri.toCharArray().map { it.code.toByte() }.toByteArray()
        }
        return null
    }

    private fun unzipFiles(array: ByteArray): Map<String, ByteArray>? {
        return try {
            ZipInputStream(ByteArrayInputStream(array)).use { zipInputStream ->
                generateSequence { zipInputStream.nextEntry }
                    .filterNot {  it.isDirectory }
                    .associate {
                        Pair(it.name, zipInputStream.readBytes())
                    }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    fun unpack(uri: String): JsonLDObject? {
        val array = prefixDecode(uri) ?: return null
        return buildJSonLDDocument(String(unzipFiles(array)?.get("certificate.json")!!))
    }

    private fun getKID(jsonld: JsonLDObject): String? {
        return (jsonld.jsonObject["proof"] as? Map<*, *>)?.get("verificationMethod") as String?
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return registry.resolve(TrustRegistry.Framework.DIVOC, kid)
    }

    private fun verify(jsonLdObject: JsonLDObject, pubKey: PublicKey): Boolean {
        return try {
            val signatureSuite = (jsonLdObject.jsonObject["proof"] as? Map<*, *>)?.get("type") as String?

            when (signatureSuite) {
                "RsaSignature2018" -> RsaSignature2018withPS256Verifier(pubKey).verify(jsonLdObject)
                "Ed25519Signature2018" -> Ed25519Signature2018Verifier(pubKey).verify(jsonLdObject)
                else -> false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun unpackAndVerify(uri: String): QRDecoder.VerificationResult {
        val array = prefixDecode(uri) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, uri)
        val json = unzipFiles(array)?.get("certificate.json")?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_COMPRESSION, null, null, uri)
        val signedMessage = buildJSonLDDocument(String(json)) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, uri)

        val mapped = map(String(json)) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, uri)

        val contents = JsonLDTranslator().toFhir(mapped)

        val kid = getKID(signedMessage) ?: return QRDecoder.VerificationResult(QRDecoder.Status.KID_NOT_INCLUDED, contents, null, uri)
        val issuer = resolveIssuer(kid) ?: return QRDecoder.VerificationResult(QRDecoder.Status.ISSUER_NOT_TRUSTED, contents, null, uri)

        return when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> QRDecoder.VerificationResult(QRDecoder.Status.TERMINATED_KEYS, contents, issuer, uri)
            TrustRegistry.Status.EXPIRED -> QRDecoder.VerificationResult(QRDecoder.Status.EXPIRED_KEYS, contents, issuer, uri)
            TrustRegistry.Status.REVOKED -> QRDecoder.VerificationResult(QRDecoder.Status.REVOKED_KEYS, contents, issuer, uri)
            TrustRegistry.Status.CURRENT ->
                if (verify(signedMessage, issuer.publicKey))
                    QRDecoder.VerificationResult(QRDecoder.Status.VERIFIED, contents, issuer, uri)
                else
                    QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNATURE, contents, issuer, uri)
        }
    }
}