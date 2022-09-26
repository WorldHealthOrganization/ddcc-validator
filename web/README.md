# World Health Organization's Web DDCC Verifier

This module is composed of UI and API for a DDCC Verifier

The DDCC Verifier is deployed here: https://ddcc-validator.pathcheck.org/

# Usage

The API has two available REST inputs: 

## 1. Image input with 

```bash
curl --form file='@QRCode.png' https://ddcc-validator.pathcheck.org/findAndVerify
```

## 2. QR Content in a POST with JSON

```bash
curl -X POST --location "https://ddcc-validator.pathcheck.org/verify" -H "Content-Type: application/json" -d "@QRCodeIn.json"
```

where QRCodeIn.json contains the string representation inside the QR Code. For example 

```json
{
"uri": "HC1:6BF6W1SX77XS%20KHH3QH0 8KQLRM86427%NFPT*N2ZNC1R6A-LGN84TEZPF5DQPL9V:JNJMQ82$ OJU3%2SUXT Y1I-0Z+GC:UW+12YE3L3PE7E8TK5MNBPGEVQR3J:DDJL:/SQ0V9L7U6IS2MQF9-Q16UG$QVW6C8AK9FHVUD%RNM3G9$EERHCVAY0QZJ8ZEA1UG4GFJ LIG4ABGKMKE1TO58NP8G50IAIF8D55RXOQNYH4ZD4YQZZIFF6+9B-DBUFGW/B S3K-OQ+11YMCE42998UII7CICK 8M398XQNWBT5DB5 C0P8 :NZ372DB3UGYVH7KSI-RKYLUNH43DL*2YR5+-DVNAFKGW+13%R+POHWBB42B:2M59YNR0B0%VODGQ+SGF1M* AUKKKSI6G43QBA/O+GH1 2Z%M200.D1%+QWZ14HC*MT6.KC8M%2LKDVV*NS*SM$8*NQ/MCYRC70CV$BWQL%V28NJEYUG*N7PGQGDS+F0:BH2GS0BEXBEA6+/GZYQPK2B2AQWNYNPZ*N8DSK/V%LFS8VIDSM*36EV JSO0T%5FCGW9US8BQV$VH QE2GZ%U75EEJH"
}
```

Users can either pass the picture to find the QR Code or find the QR code themselves first and then pass the information inside it along. 

## 3. The output

The output includes stage-by-stage information of the verification process:

- "status" -> Error codes defined [here](https://github.com/WorldHealthOrganization/ddcc-validator/blob/main/verify/src/main/java/org/who/ddccverifier/verify/QRDecoder.kt)
- "qr" -> the value in the QR. if the QR is binary (DIVOC), it outputs a Base64 of the binary content. 
- "unpacked" -> the best representation of the contents as expected by each specification
- "contents" -> the resulting FHIR Composition 
- "issuer" -> the issuer of the keys from the DID Document

```json
{
  "status" : "VERIFIED",
  "qr" : "HC1:6BF6W1SX77XS%20KHH3QH0 8KQLRM86427%NFPT*N2ZNC1R6A-LGN84TEZPF5DQPL9V:JNJMQ82$ OJU3%2SUXT Y1I-0Z+GC:UW+12YE3L3PE7E8TK5MNBPGEVQR3J:DDJL:/SQ0V9L7U6IS2MQF9-Q16UG$QVW6C8AK9FHVUD%RNM3G9$EERHCVAY0QZJ8ZEA1UG4GFJ LIG4ABGKMKE1TO58NP8G50IAIF8D55RXOQNYH4ZD4YQZZIFF6+9B-DBUFGW/B S3K-OQ+11YMCE42998UII7CICK 8M398XQNWBT5DB5 C0P8 :NZ372DB3UGYVH7KSI-RKYLUNH43DL*2YR5+-DVNAFKGW+13%R+POHWBB42B:2M59YNR0B0%VODGQ+SGF1M* AUKKKSI6G43QBA/O+GH1 2Z%M200.D1%+QWZ14HC*MT6.KC8M%2LKDVV*NS*SM$8*NQ/MCYRC70CV$BWQL%V28NJEYUG*N7PGQGDS+F0:BH2GS0BEXBEA6+/GZYQPK2B2AQWNYNPZ*N8DSK/V%LFS8VIDSM*36EV JSO0T%5FCGW9US8BQV$VH QE2GZ%U75EEJH",
  "unpacked" : "{\"1\":\"CL\",\"4\":1681430400,\"6\":1653927539,\"-260\":{\"1\":{\"v\":[{\"dn\":2,\"ma\":\"Sinovac-Biotech\",\"vp\":\"1119305005\",\"dt\":\"2022-04-14\",\"co\":\"CL\",\"ci\":\"URN:UVCI:V1:CL:8KYL4SKUQXIWYSAU97KX49XVJV\",\"mp\":\"CoronaVac\",\"is\":\"Ministerio de Salud\",\"sd\":2,\"tg\":\"840539006\"}],\"nam\":{\"fnt\":\"MARIA CARMEN DE LOS ANGELES\",\"fn\":\"Maria Carmen De los angeles\",\"gnt\":\"DEL RIO\",\"gn\":\"Del rio\"},\"ver\":\"1.3.0\",\"dob\":\"1989-12-14\"}}}",
  "contents" : "{\"resourceType\":\"Composition\",\"contained\":[{\"resourceType\":\"Patient\",\"id\":\"1\",\"name\":[{\"use\":\"official\",\"family\":\"Maria Carmen De los angeles\",\"given\":[\"Del rio\"]},{\"use\":\"official\",\"family\":\"MARIA CARMEN DE LOS ANGELES\",\"given\":[\"DEL RIO\"]}],\"birthDate\":\"1989-12-14\"},{\"resourceType\":\"Immunization\",\"id\":\"2\",\"extension\":[{\"url\":\"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineBrand\",\"valueCoding\":{\"system\":\"https://www.ema.europa.eu/en/medicines/human/EPAR/comirnaty\",\"code\":\"CoronaVac\"}},{\"url\":\"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCVaccineMarketAuthorization\",\"valueCoding\":{\"code\":\"Sinovac-Biotech\"}},{\"url\":\"https://WorldHealthOrganization.github.io/ddcc/StructureDefinition/DDCCCountryOfVaccination\",\"valueCoding\":{\"system\":\"urn:iso:std:iso:3166\",\"code\":\"CL\"}}],\"identifier\":[{\"value\":\"URN:UVCI:V1:CL:8KYL4SKUQXIWYSAU97KX49XVJV\"}],\"vaccineCode\":{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"1119305005\"}]},\"patient\":{\"reference\":\"#1\"},\"occurrenceDateTime\":\"2022-04-14\",\"manufacturer\":{\"id\":\"Sinovac-Biotech\"},\"protocolApplied\":[{\"authority\":{\"reference\":\"#3\"},\"targetDisease\":[{\"coding\":[{\"system\":\"http://snomed.info/sct\",\"code\":\"840539006\"}]}],\"doseNumberPositiveInt\":2,\"seriesDosesPositiveInt\":2}]},{\"resourceType\":\"Organization\",\"id\":\"3\",\"identifier\":[{\"value\":\"Ministerio de Salud\"}]}],\"type\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"82593-5\",\"display\":\"Immunization summary report\"}]},\"category\":[{\"coding\":[{\"code\":\"ddcc-vs\"}]}],\"subject\":{\"reference\":\"#1\"},\"author\":[{\"reference\":\"#3\"}],\"title\":\"International Certificate of Vaccination or Prophylaxis\",\"event\":[{\"period\":{\"start\":\"2022-05-30\",\"end\":\"2023-04-13\"}}],\"section\":[{\"code\":{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"11369-6\",\"display\":\"History of Immunization Narrative\"}]},\"author\":[{\"reference\":\"#3\"}],\"entry\":[{\"reference\":\"#2\"}]}]}",
  "issuer" : {
    "displayName" : {
      "en" : "Gov of Chile"
    },
    "displayLogo" : "",
    "status" : "CURRENT",
    "scope" : "ACCEPTANCE_TEST",
    "validFrom" : "2022-05-06T23:01:23.000+00:00",
    "validUntil" : "2024-05-05T23:01:23.000+00:00",
    "publicKey" : "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsG7Rt8Zs7NzNAGoCmuJJAdoJgdN5\na565v+/I0HMUPdYrzwwzE996cB6oSnryESkSZN3+Zxykq3C6M8hio+ov+Q==\n-----END PUBLIC KEY-----\n"
  }
}
```


# Development Overview

## Setup

Make sure to have the following pre-requisites installed:
1. Java 11
2. Android Studio Artic Fox+

Fork and clone this repository and import into Android Studio
```bash
git clone https://github.com/WorldHealthOrganization/ddcc-validator.git
```

Use one of the Android Studio builds to install and run the app in your device or a simulator.

## Building and Running
Build the app:
```bash
./gradlew bootRun
```

It will start spring boot server and run on port 8080

## Testing
```bash
./gradlew test
./gradlew connectedAndroidTest
```