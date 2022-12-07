package org.who.ddccverifier.services.cql

import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.cql2elm.quick.FhirLibrarySourceProvider
import org.fhir.ucum.UcumEssenceService
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Library
import org.opencds.cqf.cql.engine.serializing.CqlLibraryReaderFactory
import java.io.InputStream
import java.io.StringReader

object CqlBuilder {
  fun load(asset: InputStream): String {
    return asset.bufferedReader().use { bufferReader -> bufferReader.readText() }
  }

  /**
   * Compiles a CQL InputStream to ELM
   *
   * @param cqlText the CQL Library
   * @return a [CqlTranslator] object that contains the elm representation of the library inside it.
   */
  fun compile(cqlText: InputStream): CqlTranslator {
    return compile(load(cqlText))
  }

  /**
   * Compiles a CQL Text to ELM
   *
   * @param cqlText the CQL Library
   * @return a [CqlTranslator] object that contains the elm representation of the library inside it.
   */
  fun compile(cqlText: String): CqlTranslator {
    val modelManager = ModelManager()
    val libraryManager =
      LibraryManager(modelManager).apply {
        librarySourceLoader.registerProvider(FhirLibrarySourceProvider())
      }

    val translator =
      CqlTranslator.fromText(
        cqlText,
        modelManager,
        libraryManager,
        UcumEssenceService(this::class.java.getResourceAsStream("/ucum-essence.xml")),
        *CqlTranslatorOptions.defaultOptions().options.toTypedArray()
      )

    // Helper makes sure the test CQL compiles. Reports an error if it doesn't
    if (translator.errors.isNotEmpty()) {
      val errors =
        translator.errors
          .map { "${it.locator?.toLocator() ?: "[n/a]"}: ${it.message}" }
          .joinToString("\n")

      throw RuntimeException("Could not compile CQL File. Errors:\n$errors")
    }

    return translator
  }

  /**
   * Assembles an ELM Library exported as a JSON in to a FHIRLibrary
   *
   * @param jsonElmStr the JSON representation of the ELM Library
   * @param libName the Library name
   * @param libVersion the Library Version
   *
   * @return a FHIR Library that includes the ELM Library.
   */
  fun assembleFhirLib(
    cqlStr: String?,
    jsonElmStr: String?,
    xmlElmStr: String?,
    libName: String,
    libVersion: String
  ): Library {

    val attachmentCql =
      cqlStr?.let {
        Attachment().apply {
          contentType = "text/cql"
          data = it.toByteArray()
        }
      }

    val attachmentJson =
      jsonElmStr?.let {
        Attachment().apply {
          contentType = "application/elm+json"
          data = it.toByteArray()
        }
      }

    val attachmentXml =
      xmlElmStr?.let {
        Attachment().apply {
          contentType = "application/elm+xml"
          data = it.toByteArray()
        }
      }

    return Library().apply {
      id = "$libName-$libVersion"
      name = libName
      version = libVersion
      status = Enumerations.PublicationStatus.ACTIVE
      experimental = true
      url = "http://localhost/Library/$libName|$libVersion"
      attachmentCql?.let { addContent(it) }
      attachmentJson?.let { addContent(it) }
      attachmentXml?.let { addContent(it) }
    }
  }

  /**
   * Parses a JSON representation of an ELM Library and assembles into a FHIR Library
   *
   * @param jsonElm the JSON representation of the ELM Library
   * @return the assembled FHIR Library
   */
  fun buildJsonLib(jsonElm: InputStream): Library {
    val strLib = load(jsonElm)
    val elmLibrary =
      CqlLibraryReaderFactory.getReader("application/elm+json").read(StringReader(strLib))
    return assembleFhirLib(
      null,
      strLib,
      null,
      elmLibrary.identifier.id,
      elmLibrary.identifier.version
    )
  }

  /**
   * Compiles a CQL Text into ELM and assembles a FHIR Library that includes a Base64 representation
   * of the JSON representation of the compiled ELM Library
   *
   * @param cqlInputStream the CQL Library
   * @return the assembled FHIR Library
   */
  fun compileAndBuild(cqlInputStream: InputStream): Library {
    val cqlText = load(cqlInputStream)
    return compile(cqlText).let {
      assembleFhirLib(
        cqlText,
        it.toJson(),
        it.toXml(),
        it.toELM().identifier.id,
        it.toELM().identifier.version
      )
    }
  }
}