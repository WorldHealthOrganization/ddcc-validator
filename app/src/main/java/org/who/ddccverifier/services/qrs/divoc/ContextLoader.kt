package org.who.ddccverifier.services.qrs.divoc

import com.apicatalog.jsonld.document.Document
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.jsonld.loader.DocumentLoader
import com.apicatalog.jsonld.loader.DocumentLoaderOptions
import java.io.InputStream
import java.net.URI

class ContextLoader(val open: (String)-> InputStream?) : DocumentLoader {
    val cachedContexts = mapOf(
        uri("https://www.w3.org/2018/credentials/v1") to load("W32018CredentialsV1.json"),
        uri("https://cowin.gov.in/credentials/vaccination/v1") to load("DIVOCVaccinationContextV1.json"),
        uri("https://cowin.gov.in/credentials/vaccination/v2") to load("DIVOCVaccinationContextV2.json"),
        uri("https://divoc.prod/vaccine/credentials/vaccination/v1") to load("DIVOCVaccinationContextV2.json"),
        uri("https://divoc.lgcc.gov.lk/credentials/vaccination/v1") to load("DIVOCVaccinationContextV2.json"),
        uri("https://www.pedulilindungi.id/credentials/vaccination/v2") to load("DIVOCVaccinationContextV2.json"),
        uri("https://w3id.org/security/v1") to load("security-v1.jsonld"),
        uri("https://w3id.org/security/v2") to load("security-v2.jsonld"),
        uri("https://w3id.org/security/v3") to load("security-v3-unstable.jsonld"),
        uri("https://w3id.org/security/bbs/v1") to load("security-bbs-v1.jsonld"),
        uri("https://w3id.org/security/suites/secp256k1-2019/v1") to load("suites-secp256k1-2019.jsonld"),
        uri("https://w3id.org/security/suites/ed25519-2018/v1") to load("suites-ed25519-2018.jsonld"),
        uri("https://w3id.org/security/suites/ed25519-2020/v1") to load("suites-ed25519-2020.jsonld"),
        uri("https://w3id.org/security/suites/x25519-2019/v1") to load("suites-x25519-2019.jsonld"),
        uri("https://w3id.org/security/suites/jws-2020/v1") to load("suites-jws-2020.jsonld")
    ).onEach { (t, u) -> u.documentUrl = t }

    fun uri(str: String): URI {
        return URI.create(str)
    }

    fun load(assetName: String): JsonDocument {
        return JsonDocument.of(MediaType.JSON_LD, open(assetName))
    }

    override fun loadDocument(url: URI?, options: DocumentLoaderOptions?): Document? {
        return cachedContexts[url]
    }
}