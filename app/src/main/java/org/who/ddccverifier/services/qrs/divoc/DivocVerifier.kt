package org.who.ddccverifier.services.qrs.divoc

import android.util.Base64
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import foundation.identity.jsonld.JsonLDObject
import org.who.ddccverifier.services.trust.TrustRegistry
import java.security.PublicKey

import org.who.ddccverifier.services.qrs.QRUnpacker
import org.who.ddccverifier.services.qrs.divoc.jsonldcrypto.RsaSignature2018withPS256Verifier
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Date
import java.util.zip.ZipInputStream


class DivocVerifier(private val open: (String)-> InputStream?) {
    private val URI_SCHEMA = "B64:"

    data class W3CVC(
        @JsonProperty("@context")
        val context: List<String>,
        val type: List<String>,
        val issuer: String,
        val issuanceDate: Date,
        val nonTransferable: Boolean?,
        val credentialSubject: CredentialSubject,
        val evidence: List<Evidence>,
        val proof: Proof?
    )

    data class CredentialSubject(
        val type: String?,
        val id: String?,
        val refId: String?,
        val name: String?,
        val gender: String?,
        val age: String?,
        val nationality: String?,
        val address: Address?
    )

    data class Proof(
        val type: String?,
        val created: String?,
        val verificationMethod: String?,
        val proofPurpose: String?,
        val jws: String?
    )

    data class Address(
        val streetAddress: String?,
        val streetAddress2: String?,
        val district: String?,
        val city: String?,
        val addressRegion: String?,
        val addressCountry: String?,
        val postalCode: String?
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
        val date: Date?,
        val effectiveStart: Date?,
        val effectiveUntil: Date?,
        val dose: Int?,
        val totalDoses: Int?,
        val verifier: Verifier?,
        val facility: Facility?
    )

    data class Verifier(
        val name: String?,
    )

    data class Facility(
        val name: String?,
        val address: Address?
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
            val contexts = ContextLoader(open)
            val jsonLdObject = JsonLDObject.fromJson(str)
            jsonLdObject.documentLoader = contexts
            return jsonLdObject
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun prefixDecode(uri: String): String {
        if (uri.uppercase().startsWith(URI_SCHEMA)) {
            return uri.substring(URI_SCHEMA.length)
        }

        return uri
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
        val b64 = prefixDecode(uri)
        val array = Base64.decode(b64, Base64.DEFAULT)
        return buildJSonLDDocument(String(unzipFiles(array)?.get("certificate.json")!!))
    }

    private fun getKID(jsonld: JsonLDObject): String? {
        return (jsonld.jsonObject["proof"] as? Map<*, *>)?.get("verificationMethod") as String?
    }

    private fun resolveIssuer(kid: String): TrustRegistry.TrustedEntity? {
        return TrustRegistry.resolve(TrustRegistry.Framework.DIVOC, kid)
    }

    private fun verify(jsonLdObject: JsonLDObject, pubKey: PublicKey): Boolean {
        return try {
            val verifier = RsaSignature2018withPS256Verifier(pubKey)
            verifier.verify(jsonLdObject)
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun unpackAndVerify(uri: String): QRUnpacker.VerificationResult {
        val b64 = prefixDecode(uri)
        val array = Base64.decode(b64, Base64.DEFAULT) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_BASE45, null, null, uri)
        val json = unzipFiles(array)?.get("certificate.json")?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_ZIP, null, null, uri)
        val signedMessage = buildJSonLDDocument(String(json)) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_COSE, null, null, uri)

        val mapped = map(String(json)) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.INVALID_COSE, null, null, uri)

        val contents = JsonLDTranslator().toFhir(mapped)

        val kid = getKID(signedMessage) ?: return QRUnpacker.VerificationResult(QRUnpacker.Status.KID_NOT_INCLUDED, contents, null, uri)
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