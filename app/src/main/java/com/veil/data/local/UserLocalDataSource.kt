package com.veil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.veil.domain.model.User
import java.util.Base64
import java.util.UUID

/**
 * UserLocalDataSource
 *
 * All user identity lives here — encrypted on-device storage.
 * Nothing in here ever goes to a server except publicKey.
 *
 * On first launch:
 *   1. Generate UUID as userId
 *   2. Generate RSA keypair
 *   3. Store private key here (encrypted)
 *   4. Store public key here AND upload to Firebase
 */
class UserLocalDataSource(context: Context) {

    companion object {
        private const val PREFS_FILE     = "veil_user_prefs"
        private const val KEY_USER_ID    = "user_id"
        private const val KEY_DISPLAY    = "display_name"
        private const val KEY_PUBLIC_KEY = "public_key"
        private const val KEY_PRIVATE_KEY= "private_key"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_APP_LOCK   = "app_lock_enabled"
        private const val KEY_DISAPPEAR  = "default_disappear_seconds"
        private const val KEY_READ_RCPTS = "send_read_receipts"
    }

    // MasterKey backed by Android Keystore — hardware-protected on most modern devices
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // EncryptedSharedPreferences — AES256_SIV keys, AES256_GCM values
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Identity ──────────────────────────────────────────────────────

    /**
     * Returns the local user, creating a new anonymous identity if none exists.
     * This is the ONLY place a new identity is generated.
     */
    fun getOrCreateUser(): User {
        val existingId = prefs.getString(KEY_USER_ID, null)
        return if (existingId != null) {
            // Returning user — load stored identity
            User(
                userId      = existingId,
                displayName = prefs.getString(KEY_DISPLAY, null),
                publicKey   = prefs.getString(KEY_PUBLIC_KEY, "") ?: "",
                createdAt   = prefs.getLong(KEY_CREATED_AT, 0L)
            )
        } else {
            // First launch — generate a fresh anonymous identity
            val newId = UUID.randomUUID().toString()
            prefs.edit()
                .putString(KEY_USER_ID, newId)
                .putLong(KEY_CREATED_AT, System.currentTimeMillis())
                .apply()
            User(userId = newId)
        }
    }

    fun isFirstLaunch(): Boolean = prefs.getString(KEY_USER_ID, null) == null

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    // ── Display name (local only, optional) ───────────────────────────

    fun setDisplayName(name: String?) {
        prefs.edit().putString(KEY_DISPLAY, name).apply()
    }

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY, null)

    // ── Cryptographic keys ────────────────────────────────────────────

    fun storeKeyPair(publicKeyBase64: String, privateKeyBase64: String) {
        prefs.edit()
            .putString(KEY_PUBLIC_KEY, publicKeyBase64)
            .putString(KEY_PRIVATE_KEY, privateKeyBase64)
            .apply()
    }

    fun getPublicKey(): String? = prefs.getString(KEY_PUBLIC_KEY, null)

    fun getPrivateKey(): String? = prefs.getString(KEY_PRIVATE_KEY, null)

    fun getPrivateKeyBytes(): ByteArray? {
        val b64 = prefs.getString(KEY_PRIVATE_KEY, null) ?: return null
        return Base64.getDecoder().decode(b64)
    }

    fun hasKeyPair(): Boolean =
        prefs.getString(KEY_PUBLIC_KEY, null) != null &&
        prefs.getString(KEY_PRIVATE_KEY, null) != null

    // ── Privacy settings (all local) ──────────────────────────────────

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK, false)

    fun setDefaultDisappearSeconds(seconds: Int?) {
        // null = no expiry
        prefs.edit().putInt(KEY_DISAPPEAR, seconds ?: -1).apply()
    }

    fun getDefaultDisappearSeconds(): Int? {
        val v = prefs.getInt(KEY_DISAPPEAR, -1)
        return if (v == -1) null else v
    }

    fun setSendReadReceipts(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_READ_RCPTS, enabled).apply()
    }

    fun getSendReadReceipts(): Boolean = prefs.getBoolean(KEY_READ_RCPTS, true)

    // ── Nuclear option ────────────────────────────────────────────────

    /**
     * Delete everything — wipes local identity completely.
     * After this, the app behaves as if it was just installed.
     * User should also be told to delete their Firebase entry manually.
     */
    fun wipeAllData() {
        prefs.edit().clear().apply()
    }
}
