package org.who.ddccverifier.verify

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.StructureMap
import org.hl7.fhir.r4.utils.StructureMapUtilities
import java.io.InputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

open class BaseMapper {
    companion object {
        val simpleWorkerContext = SimpleWorkerContext()
        val utils = StructureMapUtilities(simpleWorkerContext)
        val structureMapCache = hashMapOf<String, StructureMap>()

        val processor = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
    }

    fun loadFile(mapFileName: String): InputStream? {
        return javaClass.getResourceAsStream(mapFileName)
    }

    fun addCache(mapFileName: String, mappingText: String) {
        structureMapCache.put(mapFileName, utils.parse(mappingText, ""))
    }

    fun loadMap(mapFileName: String): StructureMap? {
        if (!structureMapCache.containsKey(mapFileName)) {
            loadFile(mapFileName)?.let {
                addCache(mapFileName, it.bufferedReader().readText())
            }
        }

        return structureMapCache[mapFileName]
    }

    @OptIn(ExperimentalTime::class)
    fun run(source: Base, mapFileName: String): Bundle {
        val (structureMap, elapsedStructureMapLoad) = measureTimedValue {
            loadMap(mapFileName)
        }
        println("TIME: StructureMap Loaded in $elapsedStructureMapLoad")

        val (bundle, elapsedBundle) = measureTimedValue {
            Bundle().apply {
                utils.transform(simpleWorkerContext, source, structureMap, this)
            }
        }
        println("TIME: StructureMap Applied in $elapsedBundle")

        val (bundle2, elapsedBundleOrg) = measureTimedValue {
            // Is there a better way to load resources from references in a Bundle
            processor.parseResource(Bundle::class.java, processor.encodeResourceToString(bundle)) as Bundle
        }
        println("TIME: Bundle re-organized in $elapsedBundleOrg")

        return bundle2
    }
}