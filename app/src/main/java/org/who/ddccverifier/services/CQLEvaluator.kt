package org.who.ddccverifier.services

import ca.uhn.fhir.context.FhirContext
import org.cqframework.cql.elm.execution.Library
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.execution.*

/**
 * Evaluates a CQL expression on the DDCC Composite
 */
class CQLEvaluator(private val libraryLoader: FHIRLibraryLoader) {
    private fun loadDataProvider(assetBundle: IBaseBundle, fhirContext: FhirContext): DataProvider {
        val bundleRetrieveProvider = BundleRetrieveProvider(fhirContext, assetBundle)
        val r4ModelResolver = R4FhirModelResolver()
        return CompositeDataProvider(r4ModelResolver, bundleRetrieveProvider)
    }

    fun run(library: Library, asset: Composition, fhirContext: FhirContext): Context {
        libraryLoader.add(library)
        return run(library.identifier, asset, fhirContext)
    }

    fun run(libraryIdentifier: VersionedIdentifier, asset: Composition, fhirContext: FhirContext): Context {
        val bundle = Bundle()
        asset.contained.forEach {
            bundle.addEntry().resource = it
        }
        return run(libraryIdentifier, bundle, fhirContext)
    }

    fun run(libraryIdentifier: VersionedIdentifier, assetBundle: IBaseBundle, fhirContext: FhirContext): Context {
        val library = libraryLoader.load(libraryIdentifier)
        return run(library!!, assetBundle, fhirContext)
    }

    fun run(library: Library, assetBundle: IBaseBundle, fhirContext: FhirContext): Context {
        val context = Context(library)
        context.registerLibraryLoader(libraryLoader)
        context.registerDataProvider("http://hl7.org/fhir", loadDataProvider(assetBundle, fhirContext))
        return context
    }

    fun resolve(expression: String, libraryIdentifier: VersionedIdentifier, asset: Composition, fhirContext: FhirContext): Any? {
        val context = run(libraryIdentifier, asset, fhirContext)
        return context.resolveExpressionRef(expression).evaluate(context)
    }
}