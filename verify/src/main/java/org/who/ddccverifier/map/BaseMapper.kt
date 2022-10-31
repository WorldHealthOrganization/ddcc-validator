package org.who.ddccverifier.map

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

        return Bundle().apply {
            StructureMapUtilities(simpleWorkerContext)
                .transform(simpleWorkerContext, source, structureMap, this)
        }
    }
}