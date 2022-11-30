package org.who.ddccverifier.trust.didweb

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import foundation.identity.did.DIDDocument
import java.net.URI
import java.net.URL
import java.net.URLDecoder

class DIDWebResolver {

    val DOC_FILE = "/did.json"
    val DOC_PATH = "/.well-known/did.json"

    class DidResolution(
        val did: URI
    ) {
        var url: URL? = null
        var didDocument: DIDDocument? = null
        var didDocumentMetadata: String? = null
        var didResolutionMetadata = DidResolutionMetadata()
    }

    class DidResolutionMetadata() {
        var contentType: String? = null
        var error: String? = null
        var message: String? = null
    }

    fun toUrl(parsed: DIDParser.DidSections): URL {
        val domainPath = parsed.id.split(':')
        val url = if (domainPath.size > 1) {
            domainPath.map {
                URLDecoder.decode(it,"UTF-8")
            }.joinToString("/") + DOC_FILE
        } else {
            URLDecoder.decode(parsed.id,"UTF-8") + DOC_PATH
        }
        return URI.create("https://${url}").toURL()
    }

    fun resolve(did: URI): DidResolution? {
        if (!did.toString().startsWith("did:web")) return null

        val parsed = DIDParser().parse(did)
            ?: return DidResolution(did).apply {
                this.didResolutionMetadata.error = "invalidDid"
                this.didResolutionMetadata.message = "Not a valid Did $did"
            }

        val url = toUrl(parsed)

        var didDocument = try {
            jacksonObjectMapper().readValue(url, DIDDocument::class.java)
        } catch (error: Exception) {
            return DidResolution(did).apply {
                this.url = url
                this.didResolutionMetadata.error = "notFound"
                this.didResolutionMetadata.message = "DID must resolve to a valid https URL containing a JSON document: ${error}"
            }
        }

        val contentType = if (didDocument.contexts.isEmpty()) {
            "application/did+json"
        } else {
            "application/did+ld+json"
        }

        return DidResolution(did).apply {
            this.url = url
            this.didDocument = didDocument
            this.didResolutionMetadata.contentType = contentType
        }
    }
}