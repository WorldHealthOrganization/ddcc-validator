package org.who.ddccverifier.trust.pathcheck

import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.who.ddccverifier.trust.TrustRegistry
import java.net.URL
import java.security.Security
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]
operator fun <T> List<T>.component8() = this[7]

/**
 * Resolve Keys for Verifiers from PathCheck's CSV file
 */
class PCFTrustRegistry : TrustRegistry {
    private val PATHCHECK_URL = "https://raw.githubusercontent.com/Path-Check/trust-registry/main/registry_normalized.csv";

    // Using old java.time to keep compatibility down to Android SDK 22.
    private val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    // Builds a map of all Frameworks
    private val registry = EnumMap(TrustRegistry.Framework.values().associateWith {
        mutableMapOf<String, TrustRegistry.TrustedEntity>()
    });

    private fun decode(b64: String): String {
        return Base64.decode(b64, Base64.DEFAULT).toString(Charsets.UTF_8)
    }

    private fun parseDate(date: String): Date? {
        return if (date.isNotEmpty()) df.parse(date) else null
    }

    private fun wrapPem(pemB64: String): String {
        return "-----BEGIN PUBLIC KEY-----\n$pemB64\n-----END PUBLIC KEY-----"
    }

    fun load(csvURL: String) {
        val resultCSVStream = URL(csvURL)

        // Parsing the CSV
        val reader = resultCSVStream.openStream().bufferedReader()
        reader.forEachLine {
            val (
                specName, kid, status, displayNameB64, displayLogoB64,
                validFromISOStr, validUntilISOStr, publicKey,
            ) = it.split(",")

            try {
                registry[TrustRegistry.Framework.valueOf(specName.uppercase())]
                    ?.put(kid, TrustRegistry.TrustedEntity(
                        mapOf("en" to decode(displayNameB64)),
                        decode(displayLogoB64),
                        TrustRegistry.Status.valueOf(status.uppercase()),
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
    }

    public fun addTestKeys() {
        registry[TrustRegistry.Framework.DCC]?.put(
            "MTE=", TrustRegistry.TrustedEntity(
                mapOf("en" to "Test Keys for the WHO\'s DDCC"),
                null,
                TrustRegistry.Status.CURRENT,
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

        registry[TrustRegistry.Framework.DCC]?.put(
            "Rjene8QvRwA=", TrustRegistry.TrustedEntity(
                mapOf("en" to "Test Keys for the DCC"),
                null,
                TrustRegistry.Status.CURRENT,
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

        registry[TrustRegistry.Framework.SHC]?.put(
            "https://ekeys-qa.ny.gov/epass/doh/dvc/2021#jwuzRIxqyNdBSgy0_MPhNcA4tqqw2SFZfbnsucnhTEs", TrustRegistry.TrustedEntity(
                mapOf("en" to "NY State Test Keys"),
                null,
                TrustRegistry.Status.CURRENT,
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2022-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "HNBXsXwkatIRRXefv24KyyDKgFReim79i5Vyy1v0mbE",
                    "vEDKNjmwYTQp3w3qwQXPki97XxDQRH5cdMtS0Ri-Eg0")
            )
        )

        registry[TrustRegistry.Framework.SHC]?.put(
            "https://spec.smarthealth.cards/examples/issuer#3Kfdg-XwP-7gXyywtUfUADwBumDOPKMQx-iELL11W9s", TrustRegistry.TrustedEntity(
                mapOf("en" to "Test Keys for SHCs"),
                null,
                TrustRegistry.Status.CURRENT,
                df.parse("2021-01-01T08:00:00.000Z"),
                df.parse("2022-12-01T08:00:00.000Z"),
                KeyUtils.ecPublicKeyFromCoordinate(
                    "11XvRWy1I2S0EyJlyf_bWfw_TQ5CJJNLw78bHXNxcgw",
                    "eZXwxvO1hvCY0KucrPfKo7yAyMT6Ajc3N7OkAB6VYy8")
            )
        )
    }

    override fun init(customUrl: String) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        TrustRegistry.Framework.values().forEach {
            registry[it]?.clear()
        }

        println("Loading TrustRegistry from $customUrl");

        load(customUrl)
    }

    override fun init() {
        init(PATHCHECK_URL)
    }

    override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? {
        println("DDCCVerifer: Resolving $kid");
        return registry[framework]?.get(kid)
    }
}