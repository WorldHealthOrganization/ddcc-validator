package org.who.ddccverifier.services

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Composition
import org.opencds.cqf.cql.engine.data.CompositeDataProvider
import org.opencds.cqf.cql.engine.data.DataProvider
import org.opencds.cqf.cql.engine.fhir.model.R4FhirModelResolver
import org.opencds.cqf.cql.evaluator.engine.retrieve.BundleRetrieveProvider
import java.io.StringReader
import org.cqframework.cql.elm.execution.Library
import org.opencds.cqf.cql.engine.execution.*

class CQLEvaluator {
    private fun loadLibraryFromJXSON(jsonText: String): Library? {
        return JsonCqlLibraryReader.read(StringReader(jsonText))
    }

    private fun loadLibrary(text: String, fhirContext: FhirContext): Library? {
        return loadLibraryFromJXSON(text)
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

    fun run(libraryText: String, assetBundle: IBaseBundle, fhirContext: FhirContext): Context {
        val library = loadLibrary(libraryText, fhirContext)
        val context = Context(library)
        context.registerLibraryLoader(FHIRLibraryLoader())
        context.registerDataProvider("http://hl7.org/fhir", loadDataProvider(assetBundle, fhirContext))

        return context
    }

    fun resolve(expression: String, libraryText: String, asset: Composition, fhirContext: FhirContext): Any? {
        val context = run(libraryText, asset, fhirContext)
        return context.resolveExpressionRef(expression).evaluate(context)
    }
}