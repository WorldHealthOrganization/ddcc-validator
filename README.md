# World Health Organization's DDCC Verifier App

<img align="right" src="./docs/screenshots/3.Results.png" data-canonical-src="./docs/screenshots/3.Results.png" width="350px"/>

COVID-19 Credential Verifier app for Android using the WHO's Digital Documentation of COVID-19 Certificates. The app scans a QR code for a credential/pass, cryptographically verifies it and displays the results on the phone. No information is transmitted anywhere. Our goal is to make a Verifier App with the widest possible verification capabilities.

# Current Features

- [x] 0. Base Kotlin App
- [x] 1. Screen Flow
- [x] 1.1. Home Screen
- [x] 1.2. Camera Screen
- [x] 1.3. Result Screen
- [x] 2. Camera Development
- [x] 2.1. Manage Permissions
- [x] 2.2. Add a camera component to the Camera View
- [x] 2.3. QRs Finding and processing
- [x] 2.4. Showing QR Text on Screen
- [x] 3. QR Unpacking
- [x] 3.1. Generate Test Credentials
- [x] 3.2. Convert to Test Classes
- [x] 3.3. Unpack And Display Results
- [x] 4. Credential Verification (Off-line)
- [x] 4.1. Public Key Resolver (Key Management)
- [x] 4.2. Check Trust of Public Key
- [x] 4.3. Cryptographic verfication
- [x] 4.4. Error handling
- [x] 5. Displaying Card Info on Screen
- [x] 5.1. Display Patient Info
- [x] 5.2. Display Vaccine Info
- [x] 5.3. Display Test Result Info
- [x] 5.4. Display Error Messages
- [ ] 6. Credential Status Verification (On-line)
- [ ] 6.1. Call issuer to check status of the credential
- [ ] 6.2. Call issuer to download a new version of the Credential.
- [ ] 6.3. Screen changes to inform updates/issues
- [ ] 7. Rule Engine Integration
- [ ] 7.1. Define Rule Libraries
- [ ] 7.2. Define Example Rules
- [ ] 7.3. Define Test libraries
- [ ] 7.4. Run Rule engines on the FHIR dataset.
- [ ] 8. Key Cloak Integration
- [ ] 8.1. Define the need for screens.
- [ ] 9. DIVOC Processing.
- [ ] 9.1. Unpack and Verify
- [ ] 9.2. Trust Registry check
- [ ] 9.3. Display Info on Screen
- [ ] 10. SHC Processing
- [ ] 10.1. Unpack and Verify
- [ ] 10.2. Trust Registry check
- [ ] 10.3. Display Info on Screen
- [ ] 11. ICAO Processing.
- [ ] 11.1. Unpack and Verify
- [ ] 11.2. Trust Registry check
- [ ] 11.3. Display Info on Screen

# Development Overview

Make sure to have the following pre-requisites installed:
1. Java 11
2. Android Studio

Fork and clone this repository and import into Android Studio
```bash
git clone git@github.com:Path-Check/who-verifier-app.git
```

Use one of the Android Studio builds to install and run the app in your device or a simulator.

## How to Deploy

1. Generate a new signing key 
```
keytool -genkey -v -keystore <my-release-key.keystore> -alias <alias_name> -keyalg RSA -keysize 2048 -validity 10000
```
2. Create 4 Secret Key variables on your GitHub repository and fill in with the signing key information
   - `KEY_ALIAS` <- `<alias_name>`
   - `KEY_PASSWORD` <- `<your password>`
   - `KEY_STORE_PASSWORD` <- `<your key store password>`
   - `SIGNING_KEY` <- the data from `<my-release-key.keystore>`
3. Change the `versionCode` and `versionName` on `app/build.gradle`
4. Commit and push. 
5. Tag the commit with `v{versionName}`
6. Let the [Create Release GitHub Action](https://github.com/Path-Check/who-verifier-app/actions/workflows/create-release.yml) build a new `aab` file. 
7. Add your CHANGE LOG to the description of the new release
8. Download the `aab` file and upload it to the` PlayStore. 

# Contributing

[Issues](https://github.com/Path-Check/who-verifier-app/issues) and [pull requests](https://github.com/Path-Check/who-verifier-app/pulls) are very welcome.

# License

Copyright 2021 PathCheck Inc

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
