package org.who.ddccverifier.verify.divoc.jsonldcrypto

import com.apicatalog.jsonld.document.Document
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.jsonld.loader.DocumentLoader
import com.apicatalog.jsonld.loader.DocumentLoaderOptions
import java.net.URI

class ContextLoader : DocumentLoader {
    val cachedFiles = mapOf(
        "https://www.w3.org/2018/credentials/v1" to "W32018CredentialsV1.json",
        "https://cowin.gov.in/credentials/vaccination/v1" to "DIVOCVaccinationContextV1.json",
        "https://cowin.gov.in/credentials/vaccination/v2" to "DIVOCVaccinationContextV2.json",
        "https://divoc.prod/vaccine/credentials/vaccination/v1" to "DIVOCVaccinationContextV2.json",
        "https://divoc.lgcc.gov.lk/credentials/vaccination/v1" to "DIVOCVaccinationContextV2.json",
        "https://www.pedulilindungi.id/credentials/vaccination/v2" to "DIVOCVaccinationContextV2.json",
        "https://w3id.org/security/v1" to "security-v1.jsonld",
        "https://w3id.org/security/v2" to "security-v2.jsonld",
        "https://w3id.org/security/v3" to "security-v3-unstable.jsonld",
        "https://w3id.org/security/bbs/v1" to "security-bbs-v1.jsonld",
        "https://w3id.org/security/suites/secp256k1-2019/v1" to "suites-secp256k1-2019.jsonld",
        "https://w3id.org/security/suites/ed25519-2018/v1" to "suites-ed25519-2018.jsonld",
        "https://w3id.org/security/suites/ed25519-2020/v1" to "suites-ed25519-2020.jsonld",
        "https://w3id.org/security/suites/x25519-2019/v1" to "suites-x25519-2019.jsonld",
        "https://w3id.org/security/suites/jws-2020/v1" to "suites-jws-2020.jsonld"
    )
    
    val cachedContexts: MutableMap<URI, JsonDocument> = mutableMapOf()

    fun uri(str: String): URI {
        return URI.create(str)
    }

    fun load(assetName: String): JsonDocument {
        return JsonDocument.of(MediaType.JSON_LD, javaClass.getResourceAsStream(assetName))
    }

    override fun loadDocument(url: URI?, options: DocumentLoaderOptions?): Document? {
        checkNotNull(url)
        if (!cachedContexts.containsKey(url)) {
            val fileName = cachedFiles[url.toString()]
            val json = load(fileName!!)
            json.documentUrl = url

            cachedContexts.put(url, json)
        }
        return cachedContexts[url]
    }
}