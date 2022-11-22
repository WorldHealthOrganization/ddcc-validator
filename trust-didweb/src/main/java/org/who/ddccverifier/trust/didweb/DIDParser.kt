package org.who.ddccverifier.trust.didweb

import java.net.URI

class DIDParser {
    val ID_CHAR = "[a-zA-Z0-9_.%-]"
    val METHOD = "([a-zA-Z0-9_]+)"
    val METHOD_ID = "(${ID_CHAR}+(:${ID_CHAR}+)*)"
    val PARAM_CHAR = "[a-zA-Z0-9_.:%-]"
    val PARAM = ";${PARAM_CHAR}+=${PARAM_CHAR}*"
    val PARAMS = "((${PARAM})*)"
    val PATH = "(/[^#?]*)?"
    val QUERY = "([?][^#]*)?"
    val FRAGMENT = "(#.*)?"
    val DID_MATCHER = Regex(
            "^did:${METHOD}:${METHOD_ID}${PARAMS}${PATH}${QUERY}${FRAGMENT}$"
    )

    class DidSections(
        val did: String,
        val method: String,
        val id: String,
        val didUrl: URI
    ) {
        var params = mutableMapOf<String,String>()
        var path: String? = null
        var query: String? = null
        var fragment: String? = null
    }

    fun parse(didUrl: URI?): DidSections? {
        if (didUrl == null) return null

        val results = DID_MATCHER.matchEntire(didUrl.toString()) ?: return null

        val sections = results.groups

        return DidSections(
            "did:${sections[1]!!.value}:${sections[2]!!.value}",
            sections[1]!!.value,
            sections[2]!!.value,
            didUrl
        ).apply {
            sections[4]?.value?.let {
                if (it.isNotEmpty())
                    it.substring(1).split(';').forEach {
                        val kv = it.split('=')
                        this.params.put(kv[0], kv[1])
                    }
            }

            sections[6]?.value?.let { if (it.isNotEmpty()) this.path = it }
            sections[7]?.value?.let { if (it.isNotEmpty()) this.query = it.substring(1) }
            sections[8]?.value?.let { if (it.isNotEmpty()) this.fragment = it.substring(1) }
        }
    }

}