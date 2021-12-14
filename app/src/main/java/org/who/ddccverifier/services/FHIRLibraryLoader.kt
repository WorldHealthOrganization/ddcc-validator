package org.who.ddccverifier.services

import android.util.Log
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader
import org.opencds.cqf.cql.engine.execution.LibraryLoader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.HashMap

/**
 * Loads library files for the CQL Evaluator
 */
class FHIRLibraryLoader(private val open: (String)->InputStream?) : LibraryLoader {
    private val libraries: MutableMap<VersionedIdentifier, Library> = HashMap<VersionedIdentifier, Library>()

    fun add(library: Library): Library {
        libraries[library.identifier] = library
        return library
    }

    fun add(libraryText: InputStream): Library {
        return add(JsonCqlLibraryReader.read(InputStreamReader(libraryText)))
    }

    private fun loadFromResource(libraryIdentifier: VersionedIdentifier): Library {
        val fileName = String.format("%s-%s.json", libraryIdentifier.id, libraryIdentifier.version)
        //Log.i("Loading: ", fileName)

        val result = open(fileName)
            ?: throw IOException(String.format("Required library file %s was not found", fileName))

        return add(result)
    }

    override fun load(libraryIdentifier: VersionedIdentifier): Library {
        var library = libraries[libraryIdentifier]

        if (library == null) {
            library = loadFromResource(libraryIdentifier)
        }

        return library
    }
}