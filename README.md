<div align="center">

<br/>

```
 ██╗   ██╗███████╗██╗██╗
 ██║   ██║██╔════╝██║██║
 ██║   ██║█████╗  ██║██║
 ╚██╗ ██╔╝██╔══╝  ██║██║
  ╚████╔╝ ███████╗██║███████╗
   ╚═══╝  ╚══════╝╚═╝╚══════╝
```

**Anonymous. Encrypted. No accounts.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-green?style=flat-square)](https://developer.android.com/studio/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

<br/>

> *"The server should learn nothing useful, even if compromised."*

<br/>

[**Download APK**](#download) · [**Setup Guide**](#setup) · [**How it works**](#how-it-works) · [**Contributing**](#contributing)

<br/>

</div>

---

## What is Veil?

Veil is a privacy-first messaging app for Android. No phone number. No email. No name. You sign up with a randomly generated anonymous ID created entirely on your device — the server never learns who you are.

Built as a college learning project exploring encryption, modern Android development, and privacy engineering. Inspired by [Session](https://getsession.org) and [Signal](https://signal.org).

<br/>

## Privacy Guarantees

| What Veil stores on the server | What Veil **never** stores |
|---|---|
| Your anonymous UUID | Phone number / email / name |
| Your RSA public key | Your private key |
| Encrypted message ciphertext | Message plaintext |
| Message timestamps | Your contact list |
| GCM initialization vectors | Profile photos |

<br/>

## Features

- **Zero-account signup** — one button generates your anonymous identity locally
- **End-to-end encryption** — AES-256-GCM + RSA-2048. Server only ever sees ciphertext
- **QR contact exchange** — scan in person for verified, MITM-proof key exchange
- **Disappearing messages** — per-conversation TTL timers (30s to 24h)
- **Message requests** — unknown senders land in a separate inbox you can accept or decline
- **App lock** — biometric / PIN protection
- **Appearance customisation** — dark / light / AMOLED themes, 8 accent colors, 8 bubble colors, 5 app icons
- **Screenshot prevention** — `FLAG_SECURE` blocks screen capture and recents preview
- **No backup** — `allowBackup=false` keeps your keys off Google servers
- **Real-time** — Firebase Realtime Database, messages delivered instantly

<br/>

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM — ViewModel + StateFlow |
| **Local storage** | EncryptedSharedPreferences + EncryptedFile |
| **Encryption** | Google Tink (AES-256-GCM) + Java RSA-OAEP |
| **Backend** | Firebase Realtime Database |
| **Camera / QR** | CameraX + ZXing |
| **Biometrics** | AndroidX Biometric |
| **DI** | Manual (no Hilt) |
| **Min SDK** | 26 (Android 8.0) |

<br/>

## Encryption Architecture

```
Android Keystore (hardware-backed)
  └── MasterKey (AES256-GCM)
        ├── EncryptedSharedPreferences  ← user prefs, RSA private key
        └── EncryptedFile               ← contacts list (JSON)

RSA-2048 keypair  (generated once, on first launch)
  ├── Public key  → uploaded to Firebase
  └── Private key → stored locally, never leaves device

Per-message AES-256 session key  (ephemeral)
  ├── Encrypts message plaintext  (AES-256-GCM → ciphertext + IV)
  └── Encrypted with recipient's RSA public key → sent alongside ciphertext
```

**Send flow:**
```
plaintext
  → AES-256-GCM (random session key) → ciphertext + IV
  → RSA-OAEP (recipient's public key) → encrypted session key
  → Firebase stores: "encryptedKey::ciphertext" + IV
```

**Receive flow:**
```
Firebase delivers ciphertext
  → RSA decrypt (your private key) → session key
  → AES-256-GCM decrypt → plaintext
  → displayed in UI, never persisted
```

> **Phase 2 roadmap:** Replace RSA session key exchange with [Signal Protocol X3DH](https://signal.org/docs/specifications/x3dh/) for proper forward secrecy.

<br/>

## Project Structure

```
app/src/main/java/com/veil/
│
├── VeilApp.kt                       Application class
├── MainActivity.kt                  Entry point + Navigation graph
│
├── di/
│   ├── AppContainer.kt              Manual DI — all singletons
│   └── ViewModelFactory.kt          ViewModel factories
│
├── security/
│   └── VeilCrypto.kt                All crypto: AES-GCM, RSA, fingerprints
│
├── data/
│   ├── local/
│   │   ├── UserLocalDataSource.kt   Identity + keys (EncryptedSharedPrefs)
│   │   ├── ContactsLocalDataSource  Contact list (EncryptedFile / JSON)
│   │   ├── ConversationLocalDS.kt   Last message, unread counts
│   │   ├── SentMessageStore.kt      Sent plaintext cache
│   │   └── MessageRequestStore.kt   Pending message requests
│   ├── remote/
│   │   └── FirebaseDataSource.kt    Firebase Realtime DB wrapper
│   └── repository/
│       └── VeilRepository.kt        Single source of truth
│
├── domain/model/
│   ├── User.kt                      UUID-only user model
│   ├── Message.kt                   Message + MessageFirebase
│   ├── Contact.kt                   Local contact (nickname, pubkey)
│   ├── Conversation.kt              Home screen row model
│   └── MessageRequest.kt            Unknown sender request
│
└── ui/
    ├── theme/VeilTheme.kt           Dark/Light/AMOLED themes
    ├── components/VeilComponents.kt  Shared composables
    ├── onboarding/                  First launch — identity creation
    ├── home/                        Conversation list
    ├── chat/                        Message thread
    ├── settings/                    Privacy settings
    ├── appearance/                  Theme + color customisation
    ├── qr/                          QR code show/scan
    └── requests/                    Message requests inbox
```

<br/>

## Download

> **Note:** This is a personal learning project. It has not been audited for production use. Use accordingly.

You can download the latest APK from the [Releases Page](https://github.com/uditpandey727/veil/releases/latest).

Direct link: [Download APK](https://github.com/uditpandey727/veil/releases/latest/download/app-debug.apk)

### Build from source (recommended)

```bash
git clone https://github.com/YOUR_USERNAME/veil.git
cd veil
```

Set up Firebase (see [Setup](#setup)), then:

```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

Or open in Android Studio and click **Run**.

<br/>

## Setup

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- A Firebase account (free tier is fine)

### Step 1 — Clone the repo

```bash
git clone https://github.com/YOUR_USERNAME/veil.git
cd veil
```

### Step 2 — Create a Firebase project

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Create a new project
3. Add an Android app with package name `com.veil`
4. Download `google-services.json` and place it in `app/`

```
app/
  google-services.json        ← put it here
  google-services.json.example  ← template (already in repo)
```

### Step 3 — Enable Firebase Realtime Database

1. Firebase Console → Build → Realtime Database → Create database
2. Start in **test mode** (we'll apply proper rules next)
3. Go to the **Rules** tab and paste the contents of `docs/firebase-rules-fixed.json`
4. Click **Publish**

### Step 4 — Build and run

```bash
./gradlew assembleDebug
```

Or open in Android Studio → sync Gradle → Run.

> **Minimum device:** Android 8.0 (API 26) or higher

<br/>

## Firebase Database Rules

Paste this into Firebase Console → Realtime Database → Rules:

```json
{
  "rules": {
    "users": {
      "$userId": {
        ".read": "true",
        ".write": "!data.exists() || data.child('userId').val() === newData.child('userId').val()",
        ".validate": "newData.hasChildren(['userId', 'publicKey'])"
      }
    },
    "messages": {
      "$convId": {
        ".read": "true",
        "$messageId": {
          ".write": "true",
          ".validate": "newData.hasChildren(['messageId','senderId','receiverId','encryptedContent','iv','timestamp'])"
        }
      }
    },
    "inbox": {
      "$receiverId": {
        "$senderId": {
          ".read": "true",
          ".write": "true"
        }
      }
    },
    "$other": { ".read": "false", ".write": "false" }
  }
}
```

<br/>

## Roadmap

### ✅ Phase 1 — Complete
- [x] Anonymous UUID identity — no signup form
- [x] RSA-2048 keypair generated on device
- [x] AES-256-GCM message encryption via Google Tink
- [x] Firebase stores only ciphertext — server sees nothing useful
- [x] All 7 screens built in Jetpack Compose
- [x] Local encrypted contacts store (EncryptedFile)
- [x] Disappearing messages with per-message TTL
- [x] Biometric app lock
- [x] QR contact exchange with in-person key verification
- [x] Message requests — unknown senders in separate inbox
- [x] Real-time home screen — unread badges, live updates
- [x] Appearance customisation — themes, accent colors, bubble colors, app icons
- [x] Screenshot prevention (FLAG_SECURE)

### 🔲 Phase 2 — Signal Protocol
- [ ] X3DH key agreement (replace RSA session key exchange)
- [ ] Double Ratchet for forward secrecy
- [ ] Sealed sender (hides who messaged whom)
- [ ] Safety numbers / key verification UI

### 🔲 Phase 3 — Hardening
- [ ] Certificate pinning
- [ ] Traffic padding (hide message size)
- [ ] Self-hosted backend (Node.js + PostgreSQL)

### 🔲 Phase 4 — Stretch
- [ ] Group chats with encryption
- [ ] Voice messages (encrypted)
- [ ] Tor integration

<br/>

## Contributing

Pull requests are welcome. For major changes, open an issue first.

```bash
# Fork, then clone your fork
git clone https://github.com/YOUR_USERNAME/veil.git

# Create a branch
git checkout -b feature/your-feature

# Make changes, then
git commit -m "feat: your feature description"
git push origin feature/your-feature

# Open a Pull Request on GitHub
```

### Things to keep in mind

- This is a privacy app — any change that touches the data layer or crypto should be carefully considered
- Don't add dependencies that phone home or track users
- Keep the zero-PII principle: nothing identifying should ever reach the server

<br/>

## Security

Found a vulnerability? Please **do not** open a public issue. Instead email: `your@email.com`

This project has not undergone a professional security audit. It is a learning project and should not be used for situations where your physical safety depends on it.

<br/>

## Resources

- [Signal Protocol Whitepaper](https://signal.org/docs/specifications/doubleratchet/)
- [X3DH Key Agreement](https://signal.org/docs/specifications/x3dh/)
- [Google Tink](https://developers.google.com/tink)
- [Android EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)

<br/>

## License

```
MIT License

Copyright (c) 2026 YOUR_NAME

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

<br/>

---

<div align="center">

Built with ♥ as a college learning project

*Not audited for production use · Use at your own risk*

</div>
