package com.veil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SentMessageStore
 *
 * Stores the plaintext of messages WE sent, encrypted locally.
 *
 * Why needed:
 *   Messages are encrypted with the RECIPIENT's public key.
 *   That means the sender cannot decrypt their own sent messages
 *   when they come back from Firebase.
 *
 *   Solution: save plaintext locally at send time, keyed by messageId.
 *   When Firebase echoes the message back, look it up here.
 *
 * Storage: EncryptedSharedPreferences
 * Key:     "sent_{messageId}"
 * Value:   plaintext string
 *
 * Cleanup: entries older than 30 days are pruned on init to avoid
 *          unbounded growth.
 */
class SentMessageStore(context: Context) {

    companion object {
        private const val PREFS_FILE  = "veil_sent_messages"
        private const val PREFIX      = "sent_"
        private const val TS_PREFIX   = "ts_"
        private const val MAX_AGE_MS  = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        pruneOldEntries()
    }

    /** Save plaintext for a sent message */
    fun saveSentPlaintext(messageId: String, plaintext: String) {
        prefs.edit()
            .putString("$PREFIX$messageId", plaintext)
            .putLong("$TS_PREFIX$messageId", System.currentTimeMillis())
            .apply()
    }

    /** Get plaintext for a sent message, or null if not found */
    fun getSentPlaintext(messageId: String): String? =
        prefs.getString("$PREFIX$messageId", null)

    /** Check if we have plaintext for this messageId */
    fun hasSentPlaintext(messageId: String): Boolean =
        prefs.contains("$PREFIX$messageId")

    /** Delete a specific entry (e.g. when message is deleted) */
    fun deleteSentPlaintext(messageId: String) {
        prefs.edit()
            .remove("$PREFIX$messageId")
            .remove("$TS_PREFIX$messageId")
            .apply()
    }

    /** Wipe all sent message plaintexts */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Remove entries older than MAX_AGE_MS to prevent unbounded growth */
    private fun pruneOldEntries() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        val toDelete = prefs.all.keys
            .filter { it.startsWith(TS_PREFIX) }
            .filter { key ->
                val ts = prefs.getLong(key, 0L)
                ts < cutoff
            }
            .map { it.removePrefix(TS_PREFIX) }

        if (toDelete.isNotEmpty()) {
            prefs.edit().apply {
                toDelete.forEach { id ->
                    remove("$PREFIX$id")
                    remove("$TS_PREFIX$id")
                }
            }.apply()
        }
    }
}
