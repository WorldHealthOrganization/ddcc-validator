package org.who.ddccverifier.services

import ca.uhn.fhir.context.FhirContext
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.ExpressionDef
import org.fhir.ucum.UcumEssenceService
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import java.io.StringReader
import java.lang.IllegalArgumentException
import org.cqframework.cql.elm.execution.Library
import org.hl7.elm.r1.VersionedIdentifier
import org.opencds.cqf.cql.engine.exception.CqlException
import org.opencds.cqf.cql.engine.execution.*
import kotlin.collections.ArrayList

class CQLEvaluator {

    private val modelManager = ModelManager()
    private val libraryManager = LibraryManager(modelManager).apply {
        librarySourceLoader.registerProvider(FhirLibrarySourceProvider())
    }

    private val ucumService = UcumEssenceService(UcumEssenceService::class.java.getResourceAsStream("/ucum-essence.xml"))

    /**
     * Translate CQL to XML and loads the XML as a Library
     */
    private fun loadLibraryFromCQL(cqlText: String): Library? {
        val translator = CqlTranslator.fromText(cqlText, modelManager, libraryManager, ucumService)
        if (translator.errors.size > 0) {
            System.err.println("Translation failed due to errors:")
            val errors: ArrayList<String> = ArrayList()
            for (error in translator.errors) {
                val tb = error.locator
                val lines = if (tb == null) "[n/a]" else String.format("[%d:%d, %d:%d]",
                    tb.startLine, tb.startChar, tb.endLine, tb.endChar)
                System.err.printf("%s %s%n", lines, error.message)
                errors.add(lines + error.message)
            }
            throw IllegalArgumentException(errors.toString())
        }
        //println(translator.toJxson())
        return loadLibraryFromXML(translator.toXml())
    }

    private fun loadLibraryFromXML(xmlText: String): Library? {
        return CqlLibraryReader.read( StringReader(xmlText))
    }

    private fun loadLibraryFromJXSON(jsonText: String): Library? {
        return JsonCqlLibraryReader.read(StringReader(jsonText))
    }

    private fun loadLibrary(text: String, fhirContext: FhirContext): Library? {
        if (text.startsWith("<?xml", true))
            return loadLibraryFromXML(text)
        if (text.startsWith("{", true))
            return loadLibraryFromJXSON(text)
        return loadLibraryFromCQL(text)
    }

    private fun loadDependencyLibraries(): LibraryLoader {
        val fhirHelperSource = libraryManager.librarySourceLoader.getLibrarySource(
            VersionedIdentifier().withId("FHIRHelpers").withVersion("4.0.0"))
        val translator = CqlTranslator.fromStream(fhirHelperSource, modelManager, libraryManager, ucumService)
        val fhirHelpers = CqlLibraryReader.read(StringReader(translator.toXml()))

        return InMemoryLibraryLoader(arrayListOf(fhirHelpers))
    }

    private fun loadDataProvider(assetBundle: IBaseBundle, fhirContext: FhirContext): DataProvider {
        val bundleRetrieveProvider = BundleRetrieveProvider(fhirContext, assetBundle)
        val r4ModelResolver = R4FhirModelResolver()
        return CompositeDataProvider(r4ModelResolver, bundleRetrieveProvider)
    }

    fun run(libraryText: String, asset: Composition, fhirContext: FhirContext): Context {
        val bundle = Bundle()
        asset.contained.forEach {
            bundle.addEntry().setResource(it)
        }
        return run(libraryText, bundle, fhirContext)
    }

    fun resolveExpression(library: Library, name: String): ExpressionDef? {
        for (expressionDef in library.getStatements().getDef()) {
            if (expressionDef.name == name) {
                return expressionDef
            }
        }
        return null
    }

    fun run(libraryText: String, assetBundle: IBaseBundle, fhirContext: FhirContext): Context {
        val library = loadLibrary(libraryText, fhirContext)
        val context = Context(library)
        context.registerLibraryLoader(loadDependencyLibraries())
        context.registerDataProvider("http://hl7.org/fhir", loadDataProvider(assetBundle, fhirContext))

        //library?.let { println(resolveExpression(it, "GetFinalDose" )) }

        return context
    }

    fun resolve(expression: String, libraryText: String, asset: Composition, fhirContext: FhirContext): Any {
        val context = run(libraryText, asset, fhirContext)
        return context.resolveExpressionRef(expression).evaluate(context)
    }
}