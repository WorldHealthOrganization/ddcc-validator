package org.who.ddccverifier

import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.DDCCFormatter
import org.who.ddccverifier.services.DDCCVerifier

class QRUnpackingTest {

    @Test
    fun unpackAndVerifyQR1() {
        val qr1 = "6BF1.N/8OAP2G23ZB6S-K8+GT9IEG2UIR32S1XNUZRXT4N0P6U8/UROY27DJ44E:1DKTMHYP\$-RUW4+:4U44\$QRFZLG9BS\$HF\$2WJ54OD+ZL2R0U28G:NO%HJH30TAUYFN8SH6O/*1PBNLCTUAVC.5\$8WSN2-QD9777IC*CGA/KMKKNKR9CU3\$D%-0DK79TKMHT.JI:/8N 1NKB.AE9-04H12-HPMRNS7FKTS97+S0JF6\$8DJ:EM/KRGVNTM/2C-YBCMJN+K207OWBT23 +5NHM3OBFNK G9R8A+/HZ94MTQ3CIAFUQCA%4IO7MW\$H\$ AEXH8A3CG3-GH+W1+\$M*JM7D130C:2P EPF A-JKP10%KMDYL09GQ9S5EA7\$LN+R3GCW4P0J1PDHC.5NXN .9VWQP9N*:UK4QCD8DAJM\$TGD9ZR1WEC1FH LD 3I.*I8GN YI6QCCQ4POR4LFTE1QU0NHC \$S:DSBOJRCRRH5\$JFPQ5F+V624MAK1/JJLM7MCHWJ:/PVK957AN-N4XSN4N8ODT3UWAGA\$G*-M0R5+CSU:1AYGQ6QZU6+/JOW9::55HFW96-1PQ2IHS5YD3WA34QRZBROQ23IA9.M0EARC0AR2UPPI NXAV/E8BLKFBWQIC8\$K\$5GAR81JP6 3*PD0 IV1NTT1F.OSS3M-9P-L2+617240FVOBNO2*N2:D8A.BIQ6GCCF%1113MW4I\$CS:RK1DE 5TGO-R4C0DQUJL7NU670AWNOS5FVI4OX\$8HKUK1S3YQB:D-IF9JT0WLACP\$RVK:7Z0O4AWEZUFUG:.PC7FDY2UIFHTT/IFS6V 6MY97X:9H*2-V7:W08LQP0"
        val verified = DDCCVerifier().unpackAndVerify(qr1)
        assertNotNull(verified)
        assertEquals(DDCCVerifier.Status.VERIFIED, verified.status)
        assertEquals("{\"manufacturer\": {\"code\": \"TEST\", \"system\": \"http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem\"}, \"hw\": \"http://www.acme.org/practitioners/23\", \"centre\": \"Vaccination Site\", \"due_date\": \"2021-07-29\", \"lot\": \"PT123F\", \"dose\": 1, \"valid_from\": \"2021-07-08\", \"name\": \"Eddie Murphy\", \"disease\": {\"code\": \"840539006\", \"system\": \"http://snomed.info/sct\"}, \"sex\": {\"code\": \"male\", \"system\": \"http://hl7.org/fhir/administrative-gender\"}, \"brand\": {\"code\": \"TEST\", \"system\": \"http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem\"}, \"vaccine_valid\": \"2021-07-22\", \"hcid\": \"US111222333444555666\", \"pha\": \"wA69g8VD512TfTTdkTNSsG\", \"identifier\": \"1234567890\", \"vaccine\": {\"code\": \"1119349007\", \"system\": \"http://snomed.info/sct\"}, \"ma_holder\": {\"code\": \"TEST\", \"system\": \"http://worldhealthorganization.github.io/ddcc/CodeSystem/DDCC-Example-Test-CodeSystem\"}, \"total_doses\": 2, \"valid_until\": \"2022-07-08\", \"birthDate\": \"1986-09-19\", \"country\": {\"code\": \"USA\", \"system\": \"urn:iso:std:iso:3166\"}, \"date\": \"2021-07-08\"}",verified.contents.toString())
    }

    @Test
    fun cardResultBuilder() {
        val qr1 =
            "6BF1.N/8OAP2G23ZB6S-K8+GT9IEG2UIR32S1XNUZRXT4N0P6U8/UROY27DJ44E:1DKTMHYP\$-RUW4+:4U44\$QRFZLG9BS\$HF\$2WJ54OD+ZL2R0U28G:NO%HJH30TAUYFN8SH6O/*1PBNLCTUAVC.5\$8WSN2-QD9777IC*CGA/KMKKNKR9CU3\$D%-0DK79TKMHT.JI:/8N 1NKB.AE9-04H12-HPMRNS7FKTS97+S0JF6\$8DJ:EM/KRGVNTM/2C-YBCMJN+K207OWBT23 +5NHM3OBFNK G9R8A+/HZ94MTQ3CIAFUQCA%4IO7MW\$H\$ AEXH8A3CG3-GH+W1+\$M*JM7D130C:2P EPF A-JKP10%KMDYL09GQ9S5EA7\$LN+R3GCW4P0J1PDHC.5NXN .9VWQP9N*:UK4QCD8DAJM\$TGD9ZR1WEC1FH LD 3I.*I8GN YI6QCCQ4POR4LFTE1QU0NHC \$S:DSBOJRCRRH5\$JFPQ5F+V624MAK1/JJLM7MCHWJ:/PVK957AN-N4XSN4N8ODT3UWAGA\$G*-M0R5+CSU:1AYGQ6QZU6+/JOW9::55HFW96-1PQ2IHS5YD3WA34QRZBROQ23IA9.M0EARC0AR2UPPI NXAV/E8BLKFBWQIC8\$K\$5GAR81JP6 3*PD0 IV1NTT1F.OSS3M-9P-L2+617240FVOBNO2*N2:D8A.BIQ6GCCF%1113MW4I\$CS:RK1DE 5TGO-R4C0DQUJL7NU670AWNOS5FVI4OX\$8HKUK1S3YQB:D-IF9JT0WLACP\$RVK:7Z0O4AWEZUFUG:.PC7FDY2UIFHTT/IFS6V 6MY97X:9H*2-V7:W08LQP0"
        val verified = DDCCVerifier().unpackAndVerify(qr1)
        val card = DDCCFormatter().run(verified.contents!!)
        assertNotNull(card)
    }
}