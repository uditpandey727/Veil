package com.veil.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * VeilCrypto — all encryption/decryption lives here.
 *
 * Phase 1: AES-256-GCM using Google Tink (hardware-backed where available)
 * Phase 2 (future): Signal Protocol / X3DH for forward secrecy
 *
 * Key hierarchy:
 *   ┌─ Master key (Android Keystore, never extractable)
 *   │    └─ Encrypts EncryptedSharedPreferences
 *   │
 *   ├─ RSA keypair (generated once on first launch)
 *   │    ├─ Public key  → uploaded to Firebase (others encrypt to you)
 *   │    └─ Private key → stored in EncryptedSharedPreferences
 *   │
 *   └─ Per-conversation AES-256 session keys
 *        └─ Derived from shared secret (or ephemeral for Phase 1)
 */
class VeilCrypto(private val context: Context) {

    companion object {
        private const val KEYSET_NAME     = "veil_master_keyset"
        private const val PREF_FILE_NAME  = "veil_crypto_prefs"
        private const val KEY_TEMPLATE    = "AES256_GCM"
        private const val GCM_IV_LENGTH   = 12
        private const val GCM_TAG_LENGTH  = 128
        private const val RSA_KEY_SIZE    = 2048
    }

    // Tink AEAD primitive — backed by Android Keystore on supported devices
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
            .withMasterKeyUri("android-keystore://veil_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    // ── Symmetric encryption (for local data) ─────────────────────────

    /**
     * Encrypt arbitrary data using the device-bound master key.
     * Used for local storage (contacts, keys, etc.)
     */
    fun encryptLocal(plaintext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead.encrypt(plaintext, associatedData)
    }

    fun decryptLocal(ciphertext: ByteArray, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead.decrypt(ciphertext, associatedData)
    }

    // ── Message encryption (AES-256-GCM) ─────────────────────────────

    /**
     * Encrypt a message for transit using a shared session key.
     *
     * Phase 1: sessionKey is derived from recipient's public key (simplified RSA-OAEP)
     * Phase 2: replace with X3DH shared secret from Signal Protocol
     *
     * Returns Pair(base64Ciphertext, base64IV)
     */
    fun encryptMessage(plaintext: String, sessionKey: ByteArray): Pair<String, String> {
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sessionKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.getEncoder().encodeToString(ciphertext),
            Base64.getEncoder().encodeToString(iv)
        )
    }

    /**
     * Decrypt a received message using the shared session key.
     */
    fun decryptMessage(base64Ciphertext: String, base64IV: String, sessionKey: ByteArray): String {
        val ciphertext = Base64.getDecoder().decode(base64Ciphertext)
        val iv         = Base64.getDecoder().decode(base64IV)
        val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sessionKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // ── RSA keypair for key exchange ──────────────────────────────────

    /**
     * Generate an RSA-2048 keypair for identity and key exchange.
     * Called once on first launch — private key stored in EncryptedSharedPreferences.
     */
    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(RSA_KEY_SIZE, SecureRandom())
        return generator.generateKeyPair()
    }

    /**
     * Derive a shared AES session key using RSA-OAEP.
     *
     * Sender: encrypt a random AES key with recipient's public key
     * Receiver: decrypt with their private key to get the same AES key
     *
     * Phase 2: replace this with X3DH / Diffie-Hellman for forward secrecy
     */
    fun generateSessionKey(): ByteArray {
        return ByteArray(32).also { SecureRandom().nextBytes(it) } // 256-bit AES key
    }

    fun encryptSessionKey(sessionKey: ByteArray, recipientPublicKeyBytes: ByteArray): String {
        val publicKey = java.security.KeyFactory.getInstance("RSA")
            .generatePublic(java.security.spec.X509EncodedKeySpec(recipientPublicKeyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(sessionKey))
    }

    fun decryptSessionKey(encryptedSessionKey: String, privateKeyBytes: ByteArray): ByteArray {
        val privateKey = java.security.KeyFactory.getInstance("RSA")
            .generatePrivate(java.security.spec.PKCS8EncodedKeySpec(privateKeyBytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(Base64.getDecoder().decode(encryptedSessionKey))
    }

    // ── Key fingerprint (for QR verification) ─────────────────────────

    /**
     * Generate a human-readable fingerprint of a public key.
     * Users can compare these in-person to verify no MITM attack.
     * Format: "AB12 CD34 EF56 ..." (like Signal's safety numbers)
     */
    fun publicKeyFingerprint(publicKeyBytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash   = digest.digest(publicKeyBytes)
        return hash.take(12)
            .map { String.format("%02X", it) }
            .chunked(2)
            .joinToString(" ")
    }
}
