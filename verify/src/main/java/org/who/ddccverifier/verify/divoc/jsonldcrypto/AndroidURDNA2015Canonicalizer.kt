package org.who.ddccverifier.verify.divoc.jsonldcrypto

import com.apicatalog.jsonld.JsonLdError
import com.apicatalog.jsonld.JsonLdOptions
import com.apicatalog.jsonld.document.JsonDocument
import com.apicatalog.jsonld.http.media.MediaType
import com.apicatalog.jsonld.processor.ToRdfProcessor
import com.apicatalog.rdf.RdfDataset
import com.apicatalog.rdf.io.nquad.NQuadsWriter
import foundation.identity.jsonld.JsonLDException
import foundation.identity.jsonld.JsonLDObject
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizer
import info.weboftrust.ldsignatures.util.SHAUtil
import io.setl.rdf.normalization.RdfNormalize
import java.io.StringWriter

// Override URDNA Canonizalizer to:
// 1. Set the correct document loader for LDProof.
// 2. Block any run time code Creating JsonLDOPtions without passing the loader as a parameter
// 2.1. JsonLDOPtions default constructor initializes an HTTPClient, which is not available.
class AndroidURDNA2015Canonicalizer : Canonicalizer(listOf("urdna2015")) {

    override fun canonicalize(ldProof: LdProof, jsonLdObject: JsonLDObject): ByteArray {
        val ldProofWithoutProofValues = LdProof.builder()
            .base(ldProof)
            .defaultContexts(true)
            .build()

        // This line was added
        ldProofWithoutProofValues.documentLoader = jsonLdObject.documentLoader
        LdProof.removeLdProofValues(ldProofWithoutProofValues)

        // construct the LD object without proof
        val jsonLdObjectWithoutProof = JsonLDObject.builder()
            .base(jsonLdObject)
            .build()
        jsonLdObjectWithoutProof.documentLoader = jsonLdObject.documentLoader
        LdProof.removeFromJsonLdObject(jsonLdObjectWithoutProof)

        // canonicalize the LD proof and LD object
        val canonicalizedLdProofWithoutProofValues = normalize(ldProofWithoutProofValues,"urdna2015")
        val canonicalizedJsonLdObjectWithoutProof = normalize(jsonLdObjectWithoutProof, "urdna2015")

        // construct the canonicalization result
        val canonicalizationResult = ByteArray(64)
        System.arraycopy(SHAUtil.sha256(canonicalizedLdProofWithoutProofValues), 0, canonicalizationResult, 0, 32)
        System.arraycopy(SHAUtil.sha256(canonicalizedJsonLdObjectWithoutProof), 0, canonicalizationResult, 32,32)
        return canonicalizationResult
    }

    // Override JsonLDObject.toDataset
    fun toDataset(jsonld: JsonLDObject): RdfDataset? {
        val options = JsonLdOptions(jsonld.documentLoader)
        options.isOrdered = true
        options.isUriValidation = false
        val jsonDocument: JsonDocument = JsonDocument.of(MediaType.JSON_LD, jsonld.toJsonObject())
        return try {
            ToRdfProcessor.toRdf(jsonDocument, options)
        } catch (var5: JsonLdError) {
            throw JsonLDException(var5)
        }
    }

    // Override JsonLDObject.normalize
    // Created to avoid the need to HTTPClient on the toDataset Function
    fun normalize(jsonld: JsonLDObject, algorithm: String):String {
        var rdfDataset: RdfDataset? = toDataset(jsonld)
        rdfDataset = RdfNormalize.normalize(rdfDataset, algorithm)
        val stringWriter = StringWriter()
        val nQuadsWriter = NQuadsWriter(stringWriter)
        nQuadsWriter.write(rdfDataset)
        return stringWriter.buffer.toString()
    }
}