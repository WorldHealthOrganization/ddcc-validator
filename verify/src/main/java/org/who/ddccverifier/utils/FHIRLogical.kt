package org.who.ddccverifier.utils

import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import kotlin.reflect.full.declaredMemberProperties

open class FHIRLogical: Resource() {
    override fun copy(): Resource? { return null }
    override fun getResourceType(): ResourceType? { return null }

    private val propertiesByHash = this::class.declaredMemberProperties.associateBy { it.name.hashCode() }

    override fun getProperty(hash: Int, name: String?, checkValid: Boolean): Array<Base?> {
        return propertiesByHash[hash]?.let {
            val prop = it.getter.call(this)
            if (prop == null) {
                emptyArray()
            } else if (prop is Base) {
                arrayOf(prop)
            } else if (prop is Collection<*>) {
                if (prop.isEmpty()) {
                    emptyArray()
                } else {
                    (prop as Collection<Base?>).toTypedArray()
                }
            } else {
                emptyArray()
            }
        } ?: super.getProperty(hash, name, checkValid)
    }
}