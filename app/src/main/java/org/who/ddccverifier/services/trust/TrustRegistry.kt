package org.who.ddccverifier.services.trust

import android.util.Base64
import java.net.URL
import java.security.PublicKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Resolve Keys for Verifiers
 */
object TrustRegistry {
    // Using old java.time to keep compatibility down to Android SDK 22.
    private var df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    enum class Status(@JsonValue val status: String) {
        CURRENT("current"), EXPIRED("expired"), TERMINATED("terminated"), REVOKED("revoked")
    }
    enum class Type(@JsonValue val type: String) {
        ISSUER("issuer"), VERIFIER("verifier"), TRUST_REGISTRY("trust_registry")
    }
    enum class Framework(@JsonValue val framework: String) {
        CRED("CRED"),
        DCC("EUDCC"),
        ICAO("ICAO"),
        SHC("SmartHealthCards"),
        DIVOC("DIVOC");
        companion object {
            fun from(type: String?): Framework = values().find { it.framework == type } ?: DIVOC
        }
    }

    object DidDocumentDeserializer : JsonDeserializer<PublicKey>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): PublicKey {
            val node = p.readValueAsTree<JsonNode>()

            return if (node.isTextual) {
                // PEM File
                KeyUtils.publicKeyFromPEM(node.asText())
            } else {
                if (node.has("x"))
                    KeyUtils.ecPublicKeyFromCoordinate(node.get("x").asText(), node.get("y").asText())
                else
                    KeyUtils.rsaPublicKeyFromModulusExponent(node.get("n").asText(), node.get("e").asText())
            }
        }
    }

    data class TrustedEntity(
        val displayName: Map<String, String>,
        val displayLogo: String?,
        val entityType: Type,
        val status: Status,
        val statusDetail: String?,
        val validFromDT: Date?,
        val validUntilDT: Date?,
        @JsonDeserialize(using = DidDocumentDeserializer::class)
        val didDocument: PublicKey,
        val credentialType: List<String>,
    )

    private const val COL_FRAMEWORK = 0
    private const val COL_KID = 1
    private const val COL_TYPE = 2
    private const val COL_STATUS = 3
    private const val COL_DISPLAY_NAME = 4
    private const val COL_DISPLAY_LOGO = 5
    private const val COL_VALID_FROM = 6
    private const val COL_VALID_UNTIL = 7
    private const val COL_PUBLIC_KEY = 8

    private val registry: MutableMap<Framework, MutableMap<String, TrustedEntity>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProviderSingleton.getInstance())

        val reg = mutableMapOf<Framework, MutableMap<String, TrustedEntity>>(
            Framework.CRED to mutableMapOf(),
            Framework.ICAO to mutableMapOf(),
            Framework.DCC to mutableMapOf(),
            Framework.SHC to mutableMapOf(),
            Framework.DIVOC to mutableMapOf()
        )

        val resultCSVStream = URL("https://raw.githubusercontent.com/Path-Check/trust-registry/main/registry_normalized.csv")

        // Parsing the CSV
        val reader = BufferedReader(InputStreamReader(resultCSVStream.openStream()))
        reader.forEachLine {
            val row = it.split(",")
            val framework = Framework.from(row[COL_FRAMEWORK])
            try {
                reg[framework]?.put(row[COL_KID],
                    TrustedEntity(
                        mapOf("en" to Base64.decode(row[COL_DISPLAY_NAME], Base64.DEFAULT).toString(Charsets.UTF_8)),
                        Base64.decode(row[COL_DISPLAY_LOGO], Base64.DEFAULT).toString(Charsets.UTF_8),
                        Type.valueOf(row[COL_TYPE].uppercase()),
                        Status.valueOf(row[COL_STATUS].uppercase()),
                        "",
                        if (row[COL_VALID_FROM].isNotEmpty()) df.parse(row[COL_VALID_FROM]) else null,
                        if (row[COL_VALID_UNTIL].isNotEmpty()) df.parse(row[COL_VALID_UNTIL]) else null,
                        KeyUtils.publicKeyFromPEM("-----BEGIN PUBLIC KEY-----\n"+row[COL_PUBLIC_KEY]+"\n-----END PUBLIC KEY-----"),
                        listOf("")
                    )
                )
            } catch(t: Throwable) {
                println(row[1])
                t.printStackTrace()
            }
        }
        addTestKeys(reg)

        return@lazy reg
    }

    private fun addTestKeys(registry: MutableMap<Framework, MutableMap<String, TrustedEntity>>) {
        registry[Framework.DCC]?.put(
            "MTE=", TrustedEntity(
                mapOf("en" to "Test Keys for the WHO\'s DDCC"),
                null,
                Type.ISSUER,
                Status.CURRENT,
                "WHO Test Keys",
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2021-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "143329cce7868e416927599cf65a34f3ce2ffda55a7eca69ed8919a394d42f0f".chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray(),
                    "60f7f1a780d8a783bfb7a2dd6b2796e8128dbbcef9d3d168db9529971a36e7b9".chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
                ),
                listOf("VS")
            )
        )

        registry[Framework.SHC]?.put(
            "https://spec.smarthealth.cards/examples/issuer#3Kfdg-XwP-7gXyywtUfUADwBumDOPKMQx-iELL11W9s", TrustedEntity(
                mapOf("en" to "Test Keys for SHCs"),
                null,
                Type.ISSUER,
                Status.CURRENT,
                "SHC Test Keys",
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2021-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "11XvRWy1I2S0EyJlyf_bWfw_TQ5CJJNLw78bHXNxcgw",
                    "eZXwxvO1hvCY0KucrPfKo7yAyMT6Ajc3N7OkAB6VYy8"),
                listOf("VS")
            )
        )
    }

    fun init() {
        registry[Framework.DCC]?.get("MTE=")
    }

    fun resolve(framework: Framework, kid: String): TrustedEntity? {
        return registry[framework]?.get(kid)
    }
}