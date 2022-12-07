package org.who.ddccverifier.verify

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.test.BaseTrustRegistryTest
import org.who.ddccverifier.verify.divoc.DivocVerifier
import org.who.ddccverifier.verify.hcert.HCertVerifier
import org.who.ddccverifier.verify.icao.IcaoVerifier
import org.who.ddccverifier.verify.shc.ShcVerifier

class QRUnpackTest: BaseTrustRegistryTest() {
    private val mapper = ObjectMapper()

    private fun jsonEquals(v1: String, v2: String) {
        return assertEquals(mapper.readTree(v1), mapper.readTree(v2))
    }

    @Test
    fun unpackWHOQR1() {
        val qr1 = open("WHOQR1Contents.txt")
        val verified = HCertVerifier(registry).unpack(qr1)
        assertNotNull(verified)
        jsonEquals(open("WHOQR1Unpacked.json"), verified.toString())
    }

    @Test
    fun unpackWHOQR2() {
        val qr2 = open("WHOQR2Contents.txt")
        val verified = HCertVerifier(registry).unpack(qr2)
        assertNotNull(verified)
        jsonEquals(open("WHOQR2Unpacked.json"), verified.toString().replace(": undefined", ": null"))
    }

    @Test
    fun unpackSingaporePCR() {
        val qr2 = open("WHOSingaporePCRContents.txt")
        val verified = HCertVerifier(registry).unpack(qr2)
        assertNotNull(verified)
        jsonEquals(open("WHOSingaporePCRUnpacked.json"), verified.toString().replace(": undefined", ": null"))
    }

    @Test
    fun unpackEUQR1() {
        val qr1 = open("EUQR1Contents.txt")
        val cwt = HCertVerifier(registry).unpack(qr1)
        assertNotNull(cwt)

        val dcc = cwt!![-260][1]
        jsonEquals(open("EUQR1Unpacked.txt"), dcc.toString())
    }

    @Test
    fun unpackEUItalyAcceptanceQR() {
        val qr1 = open("EUItalyAcceptanceQRContents.txt")
        val cwt = HCertVerifier(registry).unpack(qr1)
        assertNotNull(cwt)

        val dcc = cwt!![-260][1]
        jsonEquals(open("EUItalyAcceptanceQRUnpacked.txt"), dcc.toString())
    }

    @Test
    fun unpackEUIndonesia() {
        val qr1 = open("EUIndonesiaContents.txt")
        val cwt = HCertVerifier(registry).unpack(qr1)
        assertNotNull(cwt)

        val dcc = cwt!![-260][1]
        jsonEquals(open("EUIndonesiaUnpacked.txt"), dcc.toString())
    }

    @Test
    fun unpackSHCQR1() {
        val qr1 = open("SHCQR1Contents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCQR1Unpacked.txt"), jwt)
    }

    @Test
    fun unpackSHCWAVax() {
        val qr1 = open("SHCWAVaxContents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCWAVaxUnpacked.txt"), jwt)
    }

    @Test
    fun unpackSHCTestResults() {
        val qr1 = open("SHCTestResultsContents.txt")
        val jwt = ShcVerifier(registry).unpack(qr1)
        assertEquals(open("SHCTestResultsUnpacked.txt"), jwt)
    }

    @Test
    fun unpackDIVOCQR1() {
        val qr1 = open("DIVOCQR1Contents.txt")
        val jsonld = DivocVerifier(registry).unpack(qr1)
        jsonEquals(open("DIVOCQR1Unpacked.json"), jsonld!!.toJson(true))
    }

    @Test
    fun unpackDIVOCJamaica() {
        val qr1 = open("DIVOCJamaicaContents.txt")
        val jsonld = DivocVerifier(registry).unpack(qr1)
        jsonEquals(open("DIVOCJamaicaUnpacked.json"), jsonld!!.toJson(true))
    }

    @Test
    fun unpackDIVOCIndonesia() {
        val qr1 = open("DIVOCIndonesiaContents.txt")
        val jsonld = DivocVerifier(registry).unpack(qr1)
        jsonEquals(open("DIVOCIndonesiaUnpacked.json"), jsonld!!.toJson(true))
    }

    @Test
    fun unpackICAOQR1() {
        val qr1 = open("ICAOQR1Contents.txt")
        val json = IcaoVerifier(registry).unpack(qr1)
        jsonEquals(open("ICAOQR1Unpacked.json"), json)
    }
}