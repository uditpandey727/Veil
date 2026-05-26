package com.veil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ConversationLocalDataSource
 *
 * Tracks lightweight conversation metadata:
 *   - last message timestamp
 *   - unread count
 *   - was last message mine?
 *
 * Now exposes a StateFlow<Int> tick that increments every time
 * any conversation is updated. HomeViewModel collects this and
 * re-fetches conversations, giving real-time home screen updates.
 */
class ConversationLocalDataSource(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "veil_conversations",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Increments every time any conversation data changes.
    // HomeViewModel collects this to know when to refresh the list.
    private val _updateTick = MutableStateFlow(0)
    val updateTick: StateFlow<Int> = _updateTick.asStateFlow()

    private fun notifyUpdate() {
        _updateTick.value = _updateTick.value + 1
    }

    // ── Read ──────────────────────────────────────────────────────────

    fun getLastTimestamp(contactId: String): Long =
        prefs.getLong("conv_${contactId}_ts", 0L)

    fun getUnreadCount(contactId: String): Int =
        prefs.getInt("conv_${contactId}_unread", 0)

    fun wasLastMessageMine(contactId: String): Boolean =
        prefs.getBoolean("conv_${contactId}_mine", false)

    // ── Write ─────────────────────────────────────────────────────────

    fun updateLastMessage(
        contactId      : String,
        timestamp      : Long,
        wasMine        : Boolean,
        incrementUnread: Boolean = false
    ) {
        val unread = if (incrementUnread) getUnreadCount(contactId) + 1
                     else getUnreadCount(contactId)

        prefs.edit()
            .putLong   ("conv_${contactId}_ts",     timestamp)
            .putBoolean("conv_${contactId}_mine",   wasMine)
            .putInt    ("conv_${contactId}_unread", unread)
            .apply()

        notifyUpdate()  // ← triggers home screen refresh
    }

    fun markAsRead(contactId: String) {
        prefs.edit()
            .putInt("conv_${contactId}_unread", 0)
            .apply()

        notifyUpdate()  // ← clears unread badge on home screen
    }

    fun deleteConversation(contactId: String) {
        prefs.edit()
            .remove("conv_${contactId}_ts")
            .remove("conv_${contactId}_mine")
            .remove("conv_${contactId}_unread")
            .apply()

        notifyUpdate()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        notifyUpdate()
    }
}
