package org.who.ddccverifier.verify.icao

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.crypto.impl.ECDSA
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.jcajce.util.BCJcaJceHelper
import org.bouncycastle.jce.PrincipalUtil
import org.bouncycastle.util.io.pem.PemReader
import org.erdtman.jcs.JsonCanonicalizer
import org.who.ddccverifier.QRDecoder
import org.who.ddccverifier.trust.TrustRegistry
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*


class IcaoVerifier (private val registry: TrustRegistry) {

    private val ALGOS = mapOf(
        "ES256" to "SHA256withECDSA",
        "ES384" to "SHA384withECDSA",
        "ES512" to "SHA512withECDSA"
    )

    fun unpack(uri: String): String {
        return uri
    }

    private fun parsePayload(iJson: String): IJson? {
        return try {
            jacksonObjectMapper().readValue(iJson, IJson::class.java)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun getData(iJson: String?): String? {
        return try {
            val mapper = jacksonObjectMapper()
            val tree = mapper.readTree(iJson)["data"]
            mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            mapper.writeValueAsString(tree)
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun base64URLtoBase64(thing: String): String {
        return thing.replace("-", "+").replace("_", "/")
    }

    fun getAuthorityKeyId(cert: X509Certificate): ByteArray? {
        val fullExtValue = cert.getExtensionValue(Extension.authorityKeyIdentifier.id) ?: return null
        val extValue = ASN1OctetString.getInstance(fullExtValue).octets
        return AuthorityKeyIdentifier.getInstance(extValue).keyIdentifier
    }

    /**
     * Produces a chain of certificate ID where index 0 is the certificate found int the QR.
     */
    private fun getKIDs(payload: IJson): List<String>? {
        val cert = getCertificate(payload.sig.cer.toString()) ?: return null
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val chain = cf.generateCertPath(listOf(cert))

        val kids = mutableListOf<String>()
        // Adds all Authority Key IDs if present.
        kids.addAll(chain.certificates.map {
            PrincipalUtil.getIssuerX509Principal(it as X509Certificate).getValues(BCStyle.C)[0].toString() + "#" +
            Base64.getEncoder().encodeToString(getAuthorityKeyId(it))
        })

        return kids
    }

    fun certificateFromPEM(pem: String): X509Certificate {
        val stream = ByteArrayInputStream(PemReader(StringReader(pem)).readPemObject().content)
        return BCJcaJceHelper().createCertificateFactory("X.509").generateCertificate(stream) as X509Certificate
    }

    private fun getCertificate(cer: String): X509Certificate? {
        return try {
            certificateFromPEM("-----BEGIN CERTIFICATE-----\n" + base64URLtoBase64(cer) + "\n-----END CERTIFICATE-----")
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun canonicalizePayload(json: String?): ByteArray? {
        return try {
            JsonCanonicalizer(json).encodedUTF8
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Returns the first known issuer from the Certificate Chain that is found in the registry and verifies the certificate on the QR.
     */
    private fun resolveIssuer(kids: List<String>, certificate: X509Certificate): TrustRegistry.TrustedEntity? {
        return kids.firstNotNullOfOrNull {
            val issuer = registry.resolve(TrustRegistry.Framework.ICAO, it)
            issuer?.let { it1 -> isTrusted(certificate, it1) }
            issuer
        }
    }

    private fun isSame(certificate: PublicKey, issuer: PublicKey): Boolean {
        return Base64.getEncoder().encodeToString(certificate.encoded)
            .equals(Base64.getEncoder().encodeToString(issuer.encoded))
    }

    private fun isSignedBy(certificate: X509Certificate, issuer: PublicKey): Boolean {
        return try {
            certificate.verify(issuer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isTrusted(certificate: X509Certificate, issuer: TrustRegistry.TrustedEntity): Boolean {
        return isSame(certificate.publicKey, issuer.publicKey)
            || isSignedBy(certificate, issuer.publicKey)
    }

    private fun verify(payload: IJson, data: String?, pubKey: PublicKey): Boolean {
        return try {
            val signature = Base64.getUrlDecoder().decode(payload.sig.sigvl.toString())
            val derSignature = ECDSA.transcodeSignatureToDER(signature)
            val sig = java.security.Signature.getInstance(ALGOS[payload.sig.alg.toString()], BouncyCastleProviderSingleton.getInstance())
            sig.initVerify(pubKey)
            sig.update(canonicalizePayload(data))
            return sig.verify(derSignature)
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun unpackAndVerify(qr: String): QRDecoder.VerificationResult {
        val iJSON = parsePayload(qr) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, qr, qr)
        val certificate = getCertificate(iJSON.sig.cer.toString()) ?: return QRDecoder.VerificationResult(
            QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, qr, qr)

        val contents = IcaoMapper().run(iJSON)

        val kids = getKIDs(iJSON) ?: return QRDecoder.VerificationResult(QRDecoder.Status.KID_NOT_INCLUDED, contents, null, qr, qr)
        val issuer = resolveIssuer(kids, certificate) ?: return QRDecoder.VerificationResult(
            QRDecoder.Status.ISSUER_NOT_TRUSTED, contents, null, qr, qr)

        return when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> QRDecoder.VerificationResult(QRDecoder.Status.TERMINATED_KEYS, contents, issuer, qr, qr)
            TrustRegistry.Status.EXPIRED -> QRDecoder.VerificationResult(QRDecoder.Status.EXPIRED_KEYS, contents, issuer, qr, qr)
            TrustRegistry.Status.REVOKED -> QRDecoder.VerificationResult(QRDecoder.Status.REVOKED_KEYS, contents, issuer, qr, qr)
            TrustRegistry.Status.CURRENT ->
                if (verify(iJSON, getData(qr), certificate.publicKey))
                    QRDecoder.VerificationResult(QRDecoder.Status.VERIFIED, contents, issuer, qr, qr)
                else
                    QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNATURE, contents, issuer, qr, qr)
        }
    }

}