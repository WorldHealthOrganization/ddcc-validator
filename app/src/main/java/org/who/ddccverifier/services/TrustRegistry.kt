package org.who.ddccverifier.services

import java.security.PublicKey

class TrustRegistry {
    fun resolve(kid: ByteArray): PublicKey {
        // Used to sign DDCC demo certs
        val d = "6c1382765aec5358f117733d281c1c7bdc39884d04a45a1e6c67c858bc206c19";

        return KeyLoader().ecPublicKeyFromPrivateKey(
            d.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray());
    }
}