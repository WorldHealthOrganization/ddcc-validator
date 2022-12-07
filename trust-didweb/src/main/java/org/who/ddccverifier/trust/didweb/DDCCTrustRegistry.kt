package org.who.ddccverifier.trust.didweb

import com.nimbusds.jose.jwk.AsymmetricJWK
import com.nimbusds.jose.jwk.JWK
import foundation.identity.did.VerificationMethod
import io.ipfs.multibase.Base58
import io.ipfs.multibase.Multibase
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.who.ddccverifier.trust.TrustRegistry
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.PublicKey
import java.security.Security
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

operator fun <T> List<T>.component6() = this[5]
operator fun <T> List<T>.component7() = this[6]
operator fun <T> List<T>.component8() = this[7]

/**
 * Resolve Keys for Verifiers from PathCheck's CSV file
 */
class DDCCTrustRegistry : TrustRegistry {
    companion object {
        const val registrySigningKey = "did:web:PCF.PW:1A13#WEB"
        const val baseDID = "did:web:raw.githubusercontent.com:WorldHealthOrganization:ddcc-trust:main:dist"

        const val PROD_KEY_ID = "$baseDID:prod:u:k"
        const val TEST_KEY_ID = "$baseDID:test:u:k"

        const val PROD_DID = "$baseDID:prod:u:ml:e"
        const val TEST_DID = "$baseDID:test:u:ml:e"

        val PRODUCTION_REGISTRY = TrustRegistry.RegistryEntity(TrustRegistry.Scope.PRODUCTION, URI(PROD_DID), null)
        val ACCEPTANCE_REGISTRY =  TrustRegistry.RegistryEntity(TrustRegistry.Scope.ACCEPTANCE_TEST, URI(TEST_DID), null)
    }

    // Builds a map of all Frameworks
    private val registry = mutableMapOf<URI, TrustRegistry.TrustedEntity>()

    private fun wrapPem(pemB64: String): String {
        return "-----BEGIN PUBLIC KEY-----\n$pemB64\n-----END PUBLIC KEY-----"
    }

    private fun buildPublicKey(verif: VerificationMethod): PublicKey? {
        if (verif.publicKeyJwk != null) {
            val key = JWK.parse(verif.publicKeyJwk)
            if (key is AsymmetricJWK) {
                return key.toPublicKey()
            }
        }

        if (verif.publicKeyBase64 != null) {
            val key = wrapPem(verif.publicKeyBase64)
            return KeyUtils.publicKeyFromPEM(key)
        }

        if (verif.publicKeyBase58 != null) {
            return KeyUtils.eddsaFromBytes(Base58.decode(verif.publicKeyBase58))
        }

        if (verif.publicKeyMultibase != null) {
            return KeyUtils.eddsaFromBytes(Multibase.decode(verif.publicKeyMultibase))
        }

        println("unable to load key ${verif.id}")

        return null
    }

    @OptIn(ExperimentalTime::class)
    fun load(registryURL: TrustRegistry.RegistryEntity) {
        try {
            val (didDocumentResolution, elapsedServerDownload) = measureTimedValue {
                DIDWebResolver().resolve(registryURL.resolvableURI)
            }
            println("TIME: Trust Downloaded in $elapsedServerDownload from ${registryURL.resolvableURI}")

            val elapsed = measureTimeMillis {
                didDocumentResolution?.didDocument?.verificationMethods?.forEach {
                    try {
                        val key = buildPublicKey(it)
                        if (key != null)
                            registry.put(it.id,
                                TrustRegistry.TrustedEntity(
                                    mapOf("en" to it.id.toString()),
                                    "",
                                    TrustRegistry.Status.CURRENT,
                                    registryURL.scope,
                                    null,
                                    null,
                                    key
                                )
                            )
                    } catch(t: Throwable) {
                        println("Exception while loading kid: ${it.id}")
                        t.printStackTrace()
                    }
                }
            }

            println("TIME: Trust Parsed and Loaded in ${elapsed}ms")

        } catch(t: Throwable) {
            println("Exception while loading registry from github")
            t.printStackTrace()
        }
    }

    override fun init(vararg customRegistries: TrustRegistry.RegistryEntity) {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())

        registry.clear()

        customRegistries.forEach {
            load(it)
        }
    }

    override fun init() {
        println("DID:WEB: Initializing")
        init(PRODUCTION_REGISTRY, ACCEPTANCE_REGISTRY)
    }

    override fun resolve(framework: TrustRegistry.Framework, kid: String): TrustRegistry.TrustedEntity? {
        if (kid.contains("#")) {
            val parts = kid.split("#")
            val encController = URLEncoder.encode(parts[0],"UTF-8")
            val encKid = URLEncoder.encode(parts[1],"UTF-8")
            println("DID:WEB: Resolving $kid -> $PROD_KEY_ID:$encController#$encKid")
            return registry[URI.create("$PROD_KEY_ID:$encController#$encKid")]
                ?: registry[URI.create("$TEST_KEY_ID:$encController#$encKid")]
        } else {
            val encKid = URLEncoder.encode(kid,"UTF-8")
            println("DID:WEB: Resolving $kid -> $PROD_KEY_ID:$encKid#$encKid")
            return registry[URI.create("$PROD_KEY_ID:$encKid#$encKid")]
                ?: registry[URI.create("$TEST_KEY_ID:$encKid#$encKid")]
        }
    }
}