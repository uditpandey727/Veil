# Veil — Privacy-First Messenger

Anonymous. Encrypted. No accounts.

---

## Core Philosophy

> "The server should learn nothing useful, even if compromised."

| What Veil stores on server | What Veil NEVER stores on server |
|---|---|
| User UUID | Phone number / email / name |
| RSA Public key | Private key |
| AES-GCM ciphertext | Plaintext messages |
| Message timestamps | Contact lists |
| GCM initialization vector | Profile photos |

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                 Android App                      │
│                                                  │
│  UI Layer         ViewModel         Repository   │
│  (XML Views)  ──▶ (StateFlow)  ──▶ (VeilRepo)  │
│                                         │        │
│                              ┌──────────┴──────┐ │
│                              │                 │ │
│                    UserLocalDataSource    FirebaseDataSource
│                    (EncryptedSharedPrefs) (Realtime DB)
│                              │
│                         VeilCrypto
│                         (Tink / RSA)
└─────────────────────────────────────────────────┘
```

---

## Build Phases

### ✅ Phase 1 — Foundation (YOU ARE HERE)
- [x] Anonymous UUID identity (no signup form)
- [x] RSA-2048 keypair generation on device
- [x] Private key in EncryptedSharedPreferences
- [x] Public key uploaded to Firebase
- [x] AES-256-GCM message encryption (Tink)
- [x] Firebase stores only ciphertext
- [x] Disappearing messages (TTL)
- [ ] Basic chat UI
- [ ] QR code contact exchange

### 🔲 Phase 2 — Encryption Upgrade
- [ ] Signal Protocol / X3DH key agreement
- [ ] Double Ratchet for forward secrecy
- [ ] Key verification (safety numbers)
- [ ] Sealed sender (hides who messaged whom)

### 🔲 Phase 3 — Privacy Hardening
- [ ] FLAG_SECURE (screenshot prevention)
- [ ] Biometric app lock
- [ ] No metadata logging
- [ ] Certificate pinning
- [ ] Traffic padding (hide message size)

### 🔲 Phase 4 — Stretch Goals
- [ ] Self-hosted backend (Node.js + PostgreSQL)
- [ ] Tor integration
- [ ] Group chats with encryption
- [ ] Voice messages (encrypted)

---

## Key Files

```
app/src/main/java/com/veil/
├── security/
│   └── VeilCrypto.kt           ← ALL encryption lives here
├── data/
│   ├── local/
│   │   └── UserLocalDataSource.kt  ← EncryptedSharedPreferences
│   ├── remote/
│   │   └── FirebaseDataSource.kt   ← Firebase (ciphertext only)
│   └── repository/
│       └── VeilRepository.kt       ← Wires everything together
├── domain/
│   └── model/
│       ├── User.kt             ← UUID-only user model
│       └── Message.kt          ← Encrypted message model
└── ui/
    └── screens/
        └── auth/
            └── OnboardingViewModel.kt  ← First launch flow
```

---

## Firebase Setup

1. Create a Firebase project at console.firebase.google.com
2. Add an Android app with package `com.veil`
3. Download `google-services.json` → place in `app/`
4. Enable **Realtime Database** (NOT Firestore for now)
5. Set database rules from `docs/firebase-rules.json`
6. Enable **Anonymous Auth** (Firebase Auth → Sign-in methods → Anonymous)

### Database Structure
```
veil-db/
├── users/
│   └── {uuid}/
│       ├── userId: "abc-123..."
│       └── publicKey: "MIIBIjANBgkq..."
└── messages/
    └── {sha256-conv-id}/
        └── {message-id}/
            ├── senderId: "abc-123..."
            ├── receiverId: "xyz-456..."
            ├── encryptedContent: "base64..."
            ├── iv: "base64..."
            ├── timestamp: 1234567890
            └── expiresAt: 1234568000  ← optional
```

---

## Encryption Flow

### Sending a message
```
User types "Hello"
    │
    ▼
Generate random 256-bit session key
    │
    ├── Encrypt "Hello" with session key (AES-256-GCM) → ciphertext + IV
    │
    └── Encrypt session key with recipient's RSA public key → encryptedKey
              │
              ▼
         Firebase stores: encryptedKey::ciphertext + IV
         (Server sees: random bytes — nothing useful)
```

### Receiving a message
```
Firebase delivers: encryptedKey::ciphertext + IV
    │
    ▼
Decrypt encryptedKey with my RSA private key → session key
    │
    ▼
Decrypt ciphertext with session key + IV → "Hello"
    │
    ▼
Display in UI (plaintext never stored anywhere)
```

---

## Privacy Decisions Log

| Decision | Why |
|---|---|
| UUID over phone/email | Zero identity linkage |
| EncryptedSharedPreferences | Keys hardware-protected on device |
| RSA-OAEP over symmetric | Each user has their own key — server can't decrypt |
| Firebase over self-hosted (Phase 1) | Faster to build, migrate later |
| allowBackup=false | Don't want private keys in Google backup |
| No READ_CONTACTS permission | App doesn't need it |
| conversationId = SHA-256(userIds) | No names in DB keys |

---

## Resources to Study

- [Signal Protocol Whitepaper](https://signal.org/docs/specifications/doubleratchet/)
- [X3DH Key Agreement](https://signal.org/docs/specifications/x3dh/)
- [Google Tink Docs](https://developers.google.com/tink)
- [Android EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-security/)
- Session app: [getsession.org](https://getsession.org) — study their architecture

---

## Running the Project

```bash
# Clone and open in Android Studio Hedgehog or newer
# Min SDK: 26 (Android 8.0) — needed for EncryptedSharedPreferences
# Target SDK: 34

# Add your google-services.json to app/
# Sync Gradle
# Run on device or emulator (API 26+)
```

---

*Built as a learning project. Not audited for production use.*