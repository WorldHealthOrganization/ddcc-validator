package org.who.ddccverifier

import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.DDCCFormatter
import org.who.ddccverifier.services.DDCCVerifier

class QRUnpackingTest {

    // DEMO QRs
    val qr1 = "HC1:6BF1.N/8OAP2G23ZB6S-K8+GT9IEG2UIR32S1XNUZRXT4N0P6U8/UROY27DJ44E:1DKTMHYP\$-RUW4+:4U44\$QRFZLG9BS\$HF\$2WJ54OD+ZL2R0U28G:NO%HJH30TAUYFN8SH6O/*1PBNLCTUAVC.5\$8WSN2-QD9777IC*CGA/KMKKNKR9CU3\$D%-0DK79TKMHT.JI:/8N 1NKB.AE9-04H12-HPMRNS7FKTS97+S0JF6\$8DJ:EM/KRGVNTM/2C-YBCMJN+K207OWBT23 +5NHM3OBFNK G9R8A+/HZ94MTQ3CIAFUQCA%4IO7MW\$H\$ AEXH8A3CG3-GH+W1+\$M*JM7D130C:2P EPF A-JKP10%KMDYL09GQ9S5EA7\$LN+R3GCW4P0J1PDHC.5NXN .9VWQP9N*:UK4QCD8DAJM\$TGD9ZR1WEC1FH LD 3I.*I8GN YI6QCCQ4POR4LFTE1QU0NHC \$S:DSBOJRCRRH5\$JFPQ5F+V624MAK1/JJLM7MCHWJ:/PVK957AN-N4XSN4N8ODT3UWAGA\$G*-M0R5+CSU:1AYGQ6QZU6+/JOW9::55HFW96-1PQ2IHS5YD3WA34QRZBROQ23IA9.M0EARC0AR2UPPI NXAV/E8BLKFBWQIC8\$K\$5GAR81JP6 3*PD0 IV1NTT1F.OSS3M-9P-L2+617240FVOBNO2*N2:D8A.BIQ6GCCF%1113MW4I\$CS:RK1DE 5TGO-R4C0DQUJL7NU670AWNOS5FVI4OX\$8HKUK1S3YQB:D-IF9JT0WLACP\$RVK:7Z0O4AWEZUFUG:.PC7FDY2UIFHTT/IFS6V 6MY97X:9H*2-V7:W08LQP0"
    val qr2 = "HC1:6BFOXN*TS0BI\$ZD1TH3IK0R4A S6 I-*OK%QIBRM I\$WACSQUMGVSPN\$K%OKSQ1C%O1\$Q\$M8WI16YBXN9UP81%LU IY\$NO*PB+PDNKTM8AL86H0K54%VB.KBH35UA7UV4WV4S96GZ6WFNPCNQZ6RG8.UKB\$K4I98:769PS16S45I:74N4069Q961:6G16IFNM65ZS49R5B:6PW6NY4Y65IQ5OA78L6V EBUA493L3DP.B6/D7MHG.CALBE:FXIE*GR*S5/*GB2PL/IB*0JLTZJJWT0VHH*%CC:HOMOVNVK%80G1QC82/H 9A3LEMN2UK9UF2EN9QRH:PHV-V4PQNXUTULO7REET5QBLJ39ETB6JVP0523HB3MMT03L940+MVEV4\$7OX%ISTNH7OS961+3F.582KVS1Q.D8LDA2A6G6Z5LSHPI+Q5LC\$VBMEDTJCDIDXXEYB3MHMP7QFFC- 5T0H 85*LPCB597828E:GUA0M21J\$GTY351QVVSK24C6YFI1772R*+5S UQ+DZ04K-KJRAQ+1D6WL4WCJRQUMI2TL:MUBW8H6JUULOQXIVYBWB*H.:5ZUFX7F120K5P%4"

    @Test
    fun unpackAndVerifyQR1() {
        val verified = DDCCVerifier().unpackAndVerify(qr1)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)
        assertEquals("{\"manufacturer\": {\"code\": \"TEST\", \"system\": \"http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem\"}, \"hw\": \"http://www.acme.org/practitioners/23\", \"centre\": \"Vaccination Site\", \"due_date\": \"2021-07-29\", \"lot\": \"PT123F\", \"dose\": 1, \"valid_from\": \"2021-07-08\", \"name\": \"Eddie Murphy\", \"disease\": {\"code\": \"840539006\", \"system\": \"http://snomed.info/sct\"}, \"sex\": {\"code\": \"male\", \"system\": \"http://hl7.org/fhir/administrative-gender\"}, \"brand\": {\"code\": \"TEST\", \"system\": \"http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem\"}, \"vaccine_valid\": \"2021-07-22\", \"hcid\": \"US111222333444555666\", \"pha\": \"wA69g8VD512TfTTdkTNSsG\", \"identifier\": \"1234567890\", \"vaccine\": {\"code\": \"1119349007\", \"system\": \"http://snomed.info/sct\"}, \"ma_holder\": {\"code\": \"TEST\", \"system\": \"http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem\"}, \"total_doses\": 2, \"valid_until\": \"2022-07-08\", \"birthDate\": \"1986-09-19\", \"country\": {\"code\": \"USA\", \"system\": \"urn:iso:std:iso:3166\"}, \"date\": \"2021-07-08\"}",verified.contents.toString())
    }

    @Test
    fun unpackAndVerifyQR2() {
        val verified = DDCCVerifier().unpackAndVerify(qr2)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)
        assertEquals("{\"manufacturer\": \"Organization/973\", \"centre\": \"Location/971\", \"lot\": \"PT123F.9\", \"dose\": 1, \"name\": \"EddieMurphy\", \"disease\": {\"code\": \"840539006\", \"system\": \"http://snomed.info/sct\", \"display\": \"COVID 19\"}, \"sex\": {\"code\": \"male\", \"system\": \"http://hl7.org/fhir/administrative-gender\"}, \"hcid\": \"111000111\", \"identifier\": \"111000111\", \"vaccine\": {\"code\": \"1119349007\", \"system\": \"http://snomed.info/sct\", \"display\": \"SARSCoV2  mRNA vaccine\"}, \"total_doses\": undefined, \"birthDate\": \"1986-09-19\", \"date\": undefined}",verified.contents.toString())
    }
}