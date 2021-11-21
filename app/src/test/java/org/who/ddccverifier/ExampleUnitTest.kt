package org.who.ddccverifier

import org.junit.Test

import org.junit.Assert.*
import org.who.ddccverifier.services.DDCCVerifier

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun unpackAndVerifyQR1() {
        var qr1 = "6BF1.N/8OAP2G23ZB6S-K8+GT9IEG2UIR32S1XNUZRXT4N0P6U8/UROY27DJ44E:1DKTMHYP\$-RUW4+:4U44\$QRFZLG9BS\$HF\$2WJ54OD+ZL2R0U28G:NO%HJH30TAUYFN8SH6O/*1PBNLCTUAVC.5\$8WSN2-QD9777IC*CGA/KMKKNKR9CU3\$D%-0DK79TKMHT.JI:/8N 1NKB.AE9-04H12-HPMRNS7FKTS97+S0JF6\$8DJ:EM/KRGVNTM/2C-YBCMJN+K207OWBT23 +5NHM3OBFNK G9R8A+/HZ94MTQ3CIAFUQCA%4IO7MW\$H\$ AEXH8A3CG3-GH+W1+\$M*JM7D130C:2P EPF A-JKP10%KMDYL09GQ9S5EA7\$LN+R3GCW4P0J1PDHC.5NXN .9VWQP9N*:UK4QCD8DAJM\$TGD9ZR1WEC1FH LD 3I.*I8GN YI6QCCQ4POR4LFTE1QU0NHC \$S:DSBOJRCRRH5\$JFPQ5F+V624MAK1/JJLM7MCHWJ:/PVK957AN-N4XSN4N8ODT3UWAGA\$G*-M0R5+CSU:1AYGQ6QZU6+/JOW9::55HFW96-1PQ2IHS5YD3WA34QRZBROQ23IA9.M0EARC0AR2UPPI NXAV/E8BLKFBWQIC8\$K\$5GAR81JP6 3*PD0 IV1NTT1F.OSS3M-9P-L2+617240FVOBNO2*N2:D8A.BIQ6GCCF%1113MW4I\$CS:RK1DE 5TGO-R4C0DQUJL7NU670AWNOS5FVI4OX\$8HKUK1S3YQB:D-IF9JT0WLACP\$RVK:7Z0O4AWEZUFUG:.PC7FDY2UIFHTT/IFS6V 6MY97X:9H*2-V7:W08LQP0";

        var verified = DDCCVerifier().unpackAndVerify(qr1);
        assertEquals(4, 2 + 2)

    }
    /*
    let details = {
        hcid: '6543219431654',
        name: 'Aulus Agerius',
        site: 'Vaccination Site',
        id: '0000001',
        sex: 'male',
        birthDate: '03032003',
        dose1: {
                date: '07082021',
                lot: 'PT123F',
                vaccine: 'Moderna',
                hw: 'dPD2PfwzBQyphcjeUi',
                second: true,
                date_due: '07292021'
        },
        dose2: {
                date: '07292021',
                lot: 'PT123G',
                vaccine: 'Moderna',
                hw: 'dPD2PfwzBQyphcjeUi'
        },
    }*/
}