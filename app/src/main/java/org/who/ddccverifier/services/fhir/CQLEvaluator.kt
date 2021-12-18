package org.who.ddccverifier.services.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.cqframework.cql.elm.execution.Library
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.AnnotatedUuidType
import org.opencds.cqf.cql.engine.execution.*

object LazyLoaderR4FhirModelResolver: R4FhirModelResolver() {
    override fun initialize() {
        // Override the creation of a new Context, use Cached Version instead
        fhirContext = FhirContext.forCached(FhirVersionEnum.R4)
        // do not load everything (Overriding initialize cuts 50% of evaluation time)
        fhirContext.registerCustomType(AnnotatedUuidType::class.java)
    }
}

/**
 * Evaluates a CQL expression on the DDCC Composite
 */
class CQLEvaluator(private val libraryLoader: FHIRLibraryLoader) {
    private fun loadDataProvider(assetBundle: IBaseBundle): DataProvider {
        val bundleRetrieveProvider = BundleRetrieveProvider(LazyLoaderR4FhirModelResolver.fhirContext, assetBundle)
        return CompositeDataProvider(LazyLoaderR4FhirModelResolver, bundleRetrieveProvider)
    }

    fun run(library: Library, asset: Composition): Context {
        libraryLoader.add(library)
        return run(library.identifier, asset)
    }

    fun run(libraryIdentifier: VersionedIdentifier, asset: Composition): Context {
        val bundle = Bundle()
        asset.contained.forEach {
            bundle.addEntry().resource = it
        }
        return run(libraryIdentifier, bundle)
    }

    fun run(libraryIdentifier: VersionedIdentifier, assetBundle: IBaseBundle): Context {
        val library = libraryLoader.load(libraryIdentifier)
        return run(library, assetBundle)
    }

    fun run(library: Library, assetBundle: IBaseBundle): Context {
        val context = Context(library)
        context.setExpressionCaching(true)
        context.registerLibraryLoader(libraryLoader)
        context.registerDataProvider("http://hl7.org/fhir", loadDataProvider(assetBundle))
        return context
    }

    fun resolve(expression: String, libraryIdentifier: VersionedIdentifier, asset: Composition): Any? {
        val context = run(libraryIdentifier, asset)
        return context.resolveExpressionRef(expression).evaluate(context)
    }
}