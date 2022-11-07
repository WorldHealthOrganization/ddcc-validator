package org.who.ddccverifier.services.cql

import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.execution.LibraryLoader
import org.opencds.cqf.cql.engine.serializing.jackson.JsonCqlLibraryReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.HashMap

object LibraryCache: HashMap<VersionedIdentifier, Library>(){

}

/**
 * Loads library files for the CQL Evaluator
 */
class FHIRLibraryLoader(private val open: (String)->InputStream?) : LibraryLoader {
    fun add(library: Library): Library {
        LibraryCache[library.identifier] = library
        return library
    }

    fun add(libraryText: InputStream): Library {
        return add(JsonCqlLibraryReader().read(InputStreamReader(libraryText)))
    }

    private fun loadFromResource(libraryIdentifier: VersionedIdentifier): Library {
        val fileName = String.format("%s-%s.json", libraryIdentifier.id, libraryIdentifier.version)

        val result = open(fileName)
            ?: throw IOException(String.format("Required library file %s was not found", fileName))

        return add(result)
    }

    override fun load(libraryIdentifier: VersionedIdentifier): Library {
        var library = LibraryCache[libraryIdentifier]

        if (library == null) {
            library = loadFromResource(libraryIdentifier)
        }

        return library
    }
}