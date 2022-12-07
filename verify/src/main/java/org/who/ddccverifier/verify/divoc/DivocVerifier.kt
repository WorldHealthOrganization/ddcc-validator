package org.who.ddccverifier.verify.divoc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import foundation.identity.jsonld.JsonLDObject
import org.who.ddccverifier.QRDecoder
import org.who.ddccverifier.trust.TrustRegistry
import org.who.ddccverifier.verify.divoc.jsonldcrypto.ContextLoader
import org.who.ddccverifier.verify.divoc.jsonldcrypto.Ed25519Signature2018Verifier
import org.who.ddccverifier.verify.divoc.jsonldcrypto.RsaSignature2018withPS256Verifier
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class DivocVerifier(private val registry: TrustRegistry) {
    companion object {
        val contexts = ContextLoader()
    }

    private val URI_SCHEMA = "B64:"

    private fun map(jsonStr: String): W3CVC? {
        return try {
            return jacksonObjectMapper().readValue(jsonStr, W3CVC::class.java)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun buildJSonLDDocument(str: String): JsonLDObject? {
        return try {
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

    @OptIn(ExperimentalTime::class)
    fun unpackAndVerify(uri: String): QRDecoder.VerificationResult {
        val array = prefixDecode(uri) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, uri, null)
        val json = unzipFiles(array)?.get("certificate.json")?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_COMPRESSION, null, null, uri, null)
        val signedMessage = buildJSonLDDocument(String(json)) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, uri, String(json))

        val mapped = map(String(json)) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, uri, String(json))

        val contents = DivocMapper().run(mapped)

        val kid = getKID(signedMessage) ?: return QRDecoder.VerificationResult(QRDecoder.Status.KID_NOT_INCLUDED, contents, null, uri, String(json))
        val issuer = resolveIssuer(kid) ?: return QRDecoder.VerificationResult(QRDecoder.Status.ISSUER_NOT_TRUSTED, contents, null, uri, String(json))

        return when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> QRDecoder.VerificationResult(QRDecoder.Status.TERMINATED_KEYS, contents, issuer, uri, String(json))
            TrustRegistry.Status.EXPIRED -> QRDecoder.VerificationResult(QRDecoder.Status.EXPIRED_KEYS, contents, issuer, uri, String(json))
            TrustRegistry.Status.REVOKED -> QRDecoder.VerificationResult(QRDecoder.Status.REVOKED_KEYS, contents, issuer, uri, String(json))
            TrustRegistry.Status.CURRENT -> {
                val (verified, elapsedStructureMapLoad) = measureTimedValue {
                    verify(signedMessage, issuer.publicKey)
                }
                println("TIME: Verify $elapsedStructureMapLoad")

                if (verified)
                    QRDecoder.VerificationResult(QRDecoder.Status.VERIFIED,
                        contents,
                        issuer,
                        uri,
                        String(json))
                else
                    QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNATURE,
                        contents,
                        issuer,
                        uri,
                        String(json))
            }
        }
    }
}