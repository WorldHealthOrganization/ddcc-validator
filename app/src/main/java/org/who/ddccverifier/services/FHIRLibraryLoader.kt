package org.who.ddccverifier.services

import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader
import org.opencds.cqf.cql.engine.execution.LibraryLoader
import java.io.IOException
import java.io.InputStreamReader
import java.util.HashMap

class FHIRLibraryLoader : LibraryLoader {
    private val libraries: MutableMap<String, Library> = HashMap<String, Library>()

    private fun loadFromResource(libraryIdentifier: VersionedIdentifier): Library? {
        val fileName = String.format("%s-%s.json", libraryIdentifier.id, libraryIdentifier.version)
        val result = javaClass.classLoader?.getResourceAsStream(fileName)
            ?: throw IOException(String.format("Required library file %s was not found", fileName))

        return try {
            JsonCqlLibraryReader.read(InputStreamReader(result))
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun load(libraryIdentifier: VersionedIdentifier): Library? {
        var library = libraries[libraryIdentifier.id]

        if (library == null) {
            library = loadFromResource(libraryIdentifier)
            if (library != null) {
                libraries[libraryIdentifier.id] = library
            }
        }

        return library
    }
}