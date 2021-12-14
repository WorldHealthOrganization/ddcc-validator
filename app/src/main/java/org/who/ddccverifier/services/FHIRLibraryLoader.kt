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
    private val libraries: MutableMap<String, Library> = HashMap<String, Library>()

    private fun index(libraryIdentifier: VersionedIdentifier): String {
        return String.format("%s-%s", libraryIdentifier.id, libraryIdentifier.version)
    }

    private fun loadFromResource(libraryIdentifier: VersionedIdentifier): Library? {
        val fileName = String.format("%s.json", index(libraryIdentifier))
        //Log.i("Loading: ", fileName)

        val result = open(fileName)
            ?: throw IOException(String.format("Required library file %s was not found", fileName))

        return JsonCqlLibraryReader.read(InputStreamReader(result))
    }

    override fun load(libraryIdentifier: VersionedIdentifier): Library? {
        var library = libraries[index(libraryIdentifier)]

        if (library == null) {
            library = loadFromResource(libraryIdentifier)
            if (library != null) {
                libraries[index(libraryIdentifier)] = library
            }
        }

        return library
    }

    fun add(library: Library) {
        libraries[index(library.identifier)] = library
    }

    fun add(libraryText: InputStream) {
        add(JsonCqlLibraryReader.read(InputStreamReader(libraryText)))
    }
}