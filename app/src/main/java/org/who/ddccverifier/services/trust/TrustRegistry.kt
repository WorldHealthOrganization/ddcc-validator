package org.who.ddccverifier.services.trust

import android.util.Base64
import java.net.URL
import java.security.PublicKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.who.ddccverifier.BuildConfig
import java.security.Security
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]
operator fun <T> List<T>.component8() = this[7]

/**
 * Resolve Keys for Verifiers
 */
object TrustRegistry {
    // Using old java.time to keep compatibility down to Android SDK 22.
    private var df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    enum class Status {
        CURRENT, EXPIRED, TERMINATED, REVOKED
    }
    enum class Framework {
        CRED, DCC, ICAO, SHC, DIVOC
    }

    data class TrustedEntity(
        val displayName: Map<String, String>,
        val displayLogo: String?,
        val status: Status,
        val validFromDT: Date?,
        val validUntilDT: Date?,
        val didDocument: PublicKey
    )

    private fun decode(b64: String): String {
        return Base64.decode(b64, Base64.DEFAULT).toString(Charsets.UTF_8)
    }

    private fun parseDate(date: String): Date? {
        return if (date.isNotEmpty()) df.parse(date) else null
    }

    private fun wrapPem(pemB64: String): String {
        return "-----BEGIN PUBLIC KEY-----\n$pemB64\n-----END PUBLIC KEY-----"
    }

    private val registry: Map<Framework, Map<String, TrustedEntity>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProviderSingleton.getInstance())

        val reg = EnumMap(Framework.values().associateWith { mutableMapOf<String, TrustedEntity>() })

        val resultCSVStream = URL(BuildConfig.TRUST_REGISTRY_URL)

        // Parsing the CSV
        val reader = resultCSVStream.openStream().bufferedReader()
        reader.forEachLine {
            val (specName, kid, status, displayNameB64, displayLogoB64,
                 validFromISOStr, validUntilISOStr, publicKey) = it.split(",")

            try {
                reg[Framework.valueOf(specName.uppercase())]
                    ?.put(kid, TrustedEntity(
                        mapOf("en" to decode(displayNameB64)),
                        decode(displayLogoB64),
                        Status.valueOf(status.uppercase()),
                        parseDate(validFromISOStr),
                        parseDate(validUntilISOStr),
                        KeyUtils.publicKeyFromPEM(wrapPem(publicKey))
                    )
                )
            } catch(t: Throwable) {
                println("Exception while loading kid: $specName $kid");
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
                Status.CURRENT,
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2022-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "143329cce7868e416927599cf65a34f3ce2ffda55a7eca69ed8919a394d42f0f".chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray(),
                    "60f7f1a780d8a783bfb7a2dd6b2796e8128dbbcef9d3d168db9529971a36e7b9".chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
                )
            )
        )

        registry[Framework.DCC]?.put(
            "Rjene8QvRwA=", TrustedEntity(
                mapOf("en" to "Test Keys for the DCC"),
                null,
                Status.CURRENT,
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2022-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "6425eab23bce5dc91f94ad0e44cbc11d80bc9ff4db6c0abc0e4408fa4faf3f7a".chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray(),
                    "52f5f5aa084900dbcda358263743efb5dd1ed77e9c5193415a41f3cc0f4f081b".chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
                )
            )
        )

        registry[Framework.SHC]?.put(
            "https://ekeys-qa.ny.gov/epass/doh/dvc/2021#jwuzRIxqyNdBSgy0_MPhNcA4tqqw2SFZfbnsucnhTEs", TrustedEntity(
                mapOf("en" to "NY State Test Keys"),
                null,
                Status.CURRENT,
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2022-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "HNBXsXwkatIRRXefv24KyyDKgFReim79i5Vyy1v0mbE",
                    "vEDKNjmwYTQp3w3qwQXPki97XxDQRH5cdMtS0Ri-Eg0")
            )
        )

        registry[Framework.SHC]?.put(
            "https://spec.smarthealth.cards/examples/issuer#3Kfdg-XwP-7gXyywtUfUADwBumDOPKMQx-iELL11W9s", TrustedEntity(
                mapOf("en" to "Test Keys for SHCs"),
                null,
                Status.CURRENT,
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2022-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "11XvRWy1I2S0EyJlyf_bWfw_TQ5CJJNLw78bHXNxcgw",
                    "eZXwxvO1hvCY0KucrPfKo7yAyMT6Ajc3N7OkAB6VYy8")
            )
        )
    }

    fun init() {
        registry[Framework.DCC]?.get("MTE=")
    }

    fun resolve(framework: Framework, kid: String): TrustedEntity? {
        println("DDCCVerifer: Resolving $kid");
        return registry[framework]?.get(kid)
    }
}