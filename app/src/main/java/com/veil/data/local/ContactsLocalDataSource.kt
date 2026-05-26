package com.veil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.veil.domain.model.Contact
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * ContactsLocalDataSource
 *
 * Stores the user's contact list in an AES-256-GCM encrypted JSON file.
 *
 * Why encrypted file over Room DB?
 *   - No extra dependencies
 *   - Contacts list is small (hundreds at most)
 *   - EncryptedFile uses the same Android Keystore master key as
 *     EncryptedSharedPreferences — consistent security model
 *   - Easier to understand for a learning project
 *   - Easy to migrate to Room later if needed
 *
 * File location: app's private files dir — not accessible to other apps.
 * File name: "veil_contacts.enc"
 *
 * JSON structure:
 * [
 *   {
 *     "contactId": "uuid",
 *     "nickname": "Ghost",          // optional, local only
 *     "publicKey": "base64...",
 *     "addedAt": 1234567890,
 *     "isVerified": true
 *   },
 *   ...
 * ]
 */
class ContactsLocalDataSource(private val context: Context) {

    companion object {
        private const val FILE_NAME = "veil_contacts.enc"

        // JSON keys
        private const val KEY_ID         = "contactId"
        private const val KEY_NICKNAME   = "nickname"
        private const val KEY_PUBLIC_KEY = "publicKey"
        private const val KEY_ADDED_AT   = "addedAt"
        private const val KEY_VERIFIED   = "isVerified"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedFile: EncryptedFile
        get() {
            val file = File(context.filesDir, FILE_NAME)
            return EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
        }

    // ── In-memory cache — avoids re-reading file on every call ────────
    private var cache: MutableMap<String, Contact>? = null

    // ── Read ──────────────────────────────────────────────────────────

    /**
     * Load all contacts. Returns from cache if available.
     */
    fun getAllContacts(): List<Contact> {
        return getCache().values.toList()
            .sortedBy { it.nickname ?: it.contactId }
    }

    fun getContact(contactId: String): Contact? {
        return getCache()[contactId]
    }

    fun isContact(contactId: String): Boolean {
        return getCache().containsKey(contactId)
    }

    fun getContactCount(): Int = getCache().size

    // ── Write ─────────────────────────────────────────────────────────

    /**
     * Add or update a contact. Persists immediately.
     */
    fun saveContact(contact: Contact) {
        val map = getCache()
        map[contact.contactId] = contact
        persistToFile(map)
    }

    /**
     * Update just the nickname for an existing contact.
     */
    fun updateNickname(contactId: String, nickname: String?) {
        val map  = getCache()
        val existing = map[contactId] ?: return
        map[contactId] = existing.copy(nickname = nickname)
        persistToFile(map)
    }

    /**
     * Mark a contact as key-verified (e.g. after QR scan or safety number check).
     */
    fun setVerified(contactId: String, verified: Boolean) {
        val map = getCache()
        val existing = map[contactId] ?: return
        map[contactId] = existing.copy(isVerified = verified)
        persistToFile(map)
    }

    /**
     * Remove a contact. Persists immediately.
     */
    fun deleteContact(contactId: String) {
        val map = getCache()
        map.remove(contactId)
        persistToFile(map)
    }

    /**
     * Wipe all contacts — called when user wipes the app.
     */
    fun deleteAllContacts() {
        cache = mutableMapOf()
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }

    // ── Serialization ─────────────────────────────────────────────────

    private fun getCache(): MutableMap<String, Contact> {
        if (cache != null) return cache!!
        cache = readFromFile().associateBy { it.contactId }.toMutableMap()
        return cache!!
    }

    private fun readFromFile(): List<Contact> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val json = encryptedFile.openFileInput().bufferedReader().use { it.readText() }
            parseJsonArray(json)
        } catch (e: Exception) {
            // Corrupted or unreadable file — return empty, don't crash
            emptyList()
        }
    }

    private fun persistToFile(contacts: Map<String, Contact>) {
        // EncryptedFile can't overwrite — delete first, then write fresh
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()

        val json = toJsonArray(contacts.values.toList()).toString()

        encryptedFile.openFileOutput().bufferedWriter().use { it.write(json) }
    }

    // ── JSON helpers ──────────────────────────────────────────────────

    private fun toJsonArray(contacts: List<Contact>): JSONArray {
        val array = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject().apply {
                put(KEY_ID,         contact.contactId)
                put(KEY_PUBLIC_KEY, contact.publicKey)
                put(KEY_ADDED_AT,   contact.addedAt)
                put(KEY_VERIFIED,   contact.isVerified)
                contact.nickname?.let { put(KEY_NICKNAME, it) }
            }
            array.put(obj)
        }
        return array
    }

    private fun parseJsonArray(json: String): List<Contact> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            try {
                val obj = array.getJSONObject(i)
                Contact(
                    contactId  = obj.getString(KEY_ID),
                    publicKey  = obj.getString(KEY_PUBLIC_KEY),
                    addedAt    = obj.getLong(KEY_ADDED_AT),
                    isVerified = obj.optBoolean(KEY_VERIFIED, false),
                    nickname   = if (obj.has(KEY_NICKNAME)) obj.getString(KEY_NICKNAME) else null
                )
            } catch (e: Exception) {
                null // Skip malformed entries
            }
        }
    }
}
