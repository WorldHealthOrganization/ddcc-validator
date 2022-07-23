package org.who.ddccverifier.trust.pathcheck

import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator
import org.bouncycastle.util.io.pem.PemWriter
import org.who.ddccverifier.trust.TrustRegistry
import java.io.StringWriter
import java.net.URL
import java.security.PublicKey
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
    companion object {
        const val REPO = "https://raw.githubusercontent.com/Path-Check/trust-registry/main"
        const val PATHCHECK_URL = "$REPO/registry_normalized.csv";
        const val PATHCHECK_TEST_URL = "$REPO/test_registry_normalized.csv";
    }

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

    override fun init(vararg customUrls: String) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        TrustRegistry.Framework.values().forEach {
            registry[it]?.clear()
        }

        customUrls.forEach {
            println("Loading TrustRegistry from $it");
            load(it)
        }
    }

    override fun init() {
        init(PATHCHECK_URL)
    }

    override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? {
        println("DDCCVerifer: Resolving $kid");
        return registry[framework]?.get(kid)
    }
}