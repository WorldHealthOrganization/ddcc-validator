package org.who.ddccverifier.services.qrs.icao

import android.util.Base64
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.crypto.impl.ECDSA
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.erdtman.jcs.JsonCanonicalizer
import org.who.ddccverifier.services.QRDecoder
import org.who.ddccverifier.services.trust.KeyUtils
import org.who.ddccverifier.services.trust.TrustRegistry
import java.security.MessageDigest
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.X509Name
import org.bouncycastle.its.asn1.IssuerIdentifier
import org.bouncycastle.jce.PrincipalUtil
import java.lang.Exception


class IcaoVerifier {

    private val ALGOS = mapOf(
        "ES256" to "SHA256withECDSA",
        "ES384" to "SHA384withECDSA",
        "ES512" to "SHA512withECDSA"
    )

    data class IJson(
        val data: Data,
        val sig: Signature,
    )

    data class Data(
        val hdr: Header,
        val msg: Message,
    )

    data class Header(
        @JsonProperty("is")
        val iss: String, // Issuer
        val t: String,  // Type "icao.test", "icao.vacc",
        val v: Int,      // Version
    )

    data class Message(
        val pid: Patient?,
        // For Vaccination Events
        val uvci: String?,
        val ve: List<VaccinationEvent>?,

        // For Test REsults
        val ucti: String?,
        val sp: ServiceProvider?,
        val dat: DateTimeTestReport?,
        val tr: TestResult?,
        val opt: String?, // Optional DataField
    )

    data class Patient(
        val dob: String?,
        val i: String?, // Identifier (Passport Number)
        val n: String?, // Name
        val sex: String?, // Doc 9303-4 Section 4.1.1.1 – Visual Inspection Zone M or F)
        val dt: String?, // Document Type:
                        // P – Passport (Doc 9303-4)
                        // A – ID Card (Doc 9303-5)
                        // C – ID Card (Doc 9303-5)
                        // I – ID Card Doc 9303-5)
                        // AC - Crew Member Certificate (Doc 9303-5)
                        // V – Visa (Doc 9303-7)
                        // D – Driving License (ISO18013-1)
        val dn: String?, // Document Number
        val ai: String?, // Additional Identifier
    )

    data class VaccinationEvent(
        val des: String?,  // Prophilaxis // (http://id.who.int/icd/entity/164949870)
        val dis: String?,  // Diesease or Agent Targeted (ICD-11)
        val nam: String?,  // Vaccine Brand
        val vd: List<VaccinationDetails>?,
    )

    data class VaccinationDetails(
        val adm: String?,  // Administering Center
        val ctr: String?,  // Country AUS
        val dvc: String?,  // Date of Vaccination
        val lot: String?,  // Lot #
        val seq: Int?,      // Dose Sequence
    )

    data class ServiceProvider(
        val spn: String?,  // Name of the Service Provider
        val ctr: String?,  // Country of the Test
        val cd: Contact?,  // Contact Info
    )

    data class Contact (
        val p: String?, // phone
        val e: String?, // email
        val a: String?, // address
    )

    data class DateTimeTestReport(
        val sc: String?,  // Specimen Collection Time
        val ri: String?,  // Report Issuance Time
    )

    data class TestResult(
        val tc: String?,  // Test Type (molecular(PCR), molecular(other), antigen, antibody)
        val r: String?,  // Results (positive, negative, normal, abnormal
        val m: String?,  // Sampling Method nasopharyngeal, oropharyngeal, saliva, blood, other
    )

    data class Signature(
        val alg: String,
        val cer: String,
        val sigvl: String,
    )

    fun unpack(uri: String): String {
        return uri
    }

    private fun parsePayload(iJson: String): IJson? {
        return try {
            val mapper = jacksonObjectMapper()
            mapper.readValue(iJson, IJson::class.java)
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
        val cert = getCertificate(payload.sig.cer) ?: return null;
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val chain = cf.generateCertPath(listOf(cert))

        val kids = mutableListOf<String>()
        // Adds all Authority Key IDs if present.
        kids.addAll(chain.certificates.map {
            PrincipalUtil.getIssuerX509Principal(it as X509Certificate).getValues(BCStyle.C)[0].toString() + "#" +
            Base64.encodeToString(getAuthorityKeyId(it), Base64.NO_WRAP)
        })

        return kids
    }

    private fun getCertificate(cer: String): X509Certificate? {
        return try {
            val cert = "-----BEGIN CERTIFICATE-----\n" + base64URLtoBase64(cer) + "\n-----END CERTIFICATE-----"
            KeyUtils.certificateFromPEM(cert) as X509Certificate
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    private fun canonicalizePayload(json: Data): ByteArray? {
        return try {
            val mapper = jacksonObjectMapper()
            mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            JsonCanonicalizer(mapper.writeValueAsString(json)).encodedUTF8
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
            val issuer = TrustRegistry.resolve(TrustRegistry.Framework.ICAO, it)
            issuer?.let { it1 -> isTrusted(certificate, it1) }
            issuer
        }
    }

    private fun isSame(certificate: PublicKey, issuer: PublicKey): Boolean {
        return Base64.encodeToString(certificate.encoded, Base64.NO_WRAP)
            .equals(Base64.encodeToString(issuer.encoded, Base64.NO_WRAP))
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
        return isSame(certificate.publicKey, issuer.didDocument)
            || isSignedBy(certificate, issuer.didDocument)
    }

    private fun verify(payload: IJson, pubKey: PublicKey): Boolean {
        return try {
            val signature = Base64.decode(payload.sig.sigvl, Base64.URL_SAFE)
            val derSignature = ECDSA.transcodeSignatureToDER(signature)
            val sig = java.security.Signature.getInstance(ALGOS[payload.sig.alg], BouncyCastleProviderSingleton.getInstance())
            sig.initVerify(pubKey)
            sig.update(canonicalizePayload(payload.data))
            return sig.verify(derSignature)
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun unpackAndVerify(qr: String): QRDecoder.VerificationResult {
        val iJSON = parsePayload(qr) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_ENCODING, null, null, qr)
        val certificate = getCertificate(iJSON.sig.cer) ?: return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNING_FORMAT, null, null, qr)

        val contents = IJsonTranslator().toFhir(iJSON)

        val kids = getKIDs(iJSON) ?: return QRDecoder.VerificationResult(QRDecoder.Status.KID_NOT_INCLUDED, contents, null, qr)
        val issuer = resolveIssuer(kids, certificate) ?: return QRDecoder.VerificationResult(QRDecoder.Status.ISSUER_NOT_TRUSTED, contents, null, qr)

        when (issuer.status) {
            TrustRegistry.Status.TERMINATED -> return QRDecoder.VerificationResult(QRDecoder.Status.TERMINATED_KEYS, contents, issuer, qr)
            TrustRegistry.Status.EXPIRED -> return QRDecoder.VerificationResult(QRDecoder.Status.EXPIRED_KEYS, contents, issuer, qr)
            TrustRegistry.Status.REVOKED -> return QRDecoder.VerificationResult(QRDecoder.Status.REVOKED_KEYS, contents, issuer, qr)
        }

        if (verify(iJSON, certificate.publicKey)) {
            return QRDecoder.VerificationResult(QRDecoder.Status.VERIFIED, contents, issuer, qr)
        }

        return QRDecoder.VerificationResult(QRDecoder.Status.INVALID_SIGNATURE, contents, issuer, qr)
    }

}