package org.who.ddccverifier.verify

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.r4.context.SimpleWorkerContext
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.utils.StructureMapUtilities
import java.io.InputStream

open class BaseMapper {
    fun run(source: Base, map: InputStream): Bundle {
        val simpleWorkerContext = SimpleWorkerContext()

        val structureMap = StructureMapUtilities(simpleWorkerContext).parse(
            map.bufferedReader().readText(),
            ""
        )

        val bundle = Bundle().apply {
            StructureMapUtilities(simpleWorkerContext)
                .transform(simpleWorkerContext, source, structureMap, this)
        }

        // Is there a better way to load resources from references in a Bundle
        val processor = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
        val strBundle = processor.encodeResourceToString(bundle)

        return processor.parseResource(Bundle::class.java, strBundle)
    }
}