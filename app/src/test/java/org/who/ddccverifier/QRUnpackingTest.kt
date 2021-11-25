package org.who.ddccverifier

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.DDCCFormatter
import org.who.ddccverifier.services.DDCCVerifier
import java.nio.file.Files
import kotlin.io.path.Path

class QRUnpackingTest {
    val mapper = ObjectMapper()

    fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    fun open(assetName: String): String {
        return Files.newBufferedReader(Path("./", "src/androidTest/assets/").resolve(assetName))
            .use { bufferReader -> bufferReader?.readText() } ?: ""
    }

    @Test
    fun unpackAndVerifyQR1() {
        val qr1 = open("QR1Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr1)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)
        jsonEquals(open("QR1Unpacked.json"), verified.contents.toString())
    }

    @Test
    fun unpackAndVerifyQR2() {
        val qr2 = open("QR2Contents.txt")
        val verified = DDCCVerifier().unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)
        jsonEquals(open("QR2Unpacked.json"), verified.contents.toString().replace(": undefined", ": null"))
    }
}