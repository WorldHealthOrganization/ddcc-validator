package org.who.ddccverifier

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.x509.Certificate
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.Security
import java.security.cert.CertificateFactory

/**
 * Download certificates from: https://www.icao.int/Security/FAL/PKD/Pages/icao-master-list.aspx
 * The Health Master list signing certificates ("ICAOHealthMLSigner1.pem.crt") are also available in the website.
 */
@RunWith(AndroidJUnit4::class)
class ICAOMasterListTest {

    private val cf = CertificateFactory.getInstance("X.509")!!;

    private fun inputStream(assetName: String): InputStream? {
        return javaClass.classLoader?.getResourceAsStream(assetName)
    }

    private fun loadMasterList(cmsSignedData: CMSSignedData): MutableList<java.security.cert.Certificate> {
        val cscaCerts = mutableListOf<java.security.cert.Certificate>()

        val idMasterList: String = cmsSignedData.signedContentTypeOID
        var octets: ByteArray? = null
        if (idMasterList == "2.23.136.1.1.2") {
            val bout = ByteArrayOutputStream()
            cmsSignedData.signedContent.write(bout)
            octets = bout.toByteArray()
        }

        val derObjects = ASN1Sequence.getInstance(octets).objects
        while (derObjects.hasMoreElements()) {
            val version = derObjects.nextElement() as ASN1Integer //Should be 0
            val certSet = ASN1Set.getInstance(derObjects.nextElement())
            val certs = certSet.objects;
            while (certs.hasMoreElements()) {
                val certAsASN1Object = Certificate.getInstance(certs.nextElement())
                cscaCerts.add(cf.generateCertificate(ByteArrayInputStream(certAsASN1Object.encoded)))
            }
        }
        return cscaCerts
    }

    private fun loadSignerList(cmsSignedData: CMSSignedData): MutableList<java.security.cert.Certificate> {
        val signerCertificates = mutableListOf<java.security.cert.Certificate>()
        val certStore = cmsSignedData.certificates

        val converter = JcaX509CertificateConverter()
        val certificateHolders = certStore.getMatches(null)

        for (holder in certificateHolders) {
            signerCertificates.add(converter.getCertificate(holder))
        }
        return signerCertificates
    }

    private fun verifySignerInfo(signerInfo: SignerInformation, certs: MutableCollection<out java.security.cert.Certificate>): Boolean {
        return signerInfo
            .verify(JcaSimpleSignerInfoVerifierBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(certs.first().publicKey))
    }

    @Test
    fun parseICAOHealthList() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProviderSingleton.getInstance())

        val signingCerts = cf.generateCertificates(inputStream("ICAOHealthMLSigner1.pem.crt"))

        val healthList = CMSSignedData(inputStream("ICAOHealthML27May2022.ml"))
        val signerInfo = healthList.signerInfos.signers.first()
        val cscaCerts = loadMasterList(healthList)

        Assert.assertTrue(verifySignerInfo(signerInfo, signingCerts))
        Assert.assertEquals(9, cscaCerts.size)
    }

    @Test
    fun parseICAOMasterList() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProviderSingleton.getInstance())

        val masterList = CMSSignedData(inputStream("ICAOMLJune2022.ml"))
        val signerInfo = masterList.signerInfos.signers.first()
        val cscaCerts = loadMasterList(masterList)
        val signerCertificates = loadSignerList(masterList)

        Assert.assertTrue(verifySignerInfo(signerInfo, signerCertificates))
        Assert.assertEquals(346, cscaCerts.size)
    }
}