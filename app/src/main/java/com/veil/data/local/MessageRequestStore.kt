package com.veil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.veil.domain.model.MessageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * MessageRequestStore
 *
 * Stores pending message requests locally.
 * A request is created when a message arrives from someone
 * not in the contacts list.
 *
 * Exposes a StateFlow so the home screen badge updates in real time.
 */
class MessageRequestStore(context: Context) {

    companion object {
        private const val PREFS_FILE = "veil_msg_requests"
        private const val KEY_REQUESTS = "requests_json"
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

    private val _requests = MutableStateFlow(loadRequests())
    val requests: StateFlow<List<MessageRequest>> = _requests.asStateFlow()

    val count: Int get() = _requests.value.size

    // ── Read ──────────────────────────────────────────────────────────

    fun getRequest(senderId: String): MessageRequest? =
        _requests.value.firstOrNull { it.senderId == senderId }

    fun hasRequest(senderId: String): Boolean =
        _requests.value.any { it.senderId == senderId }

    // ── Write ─────────────────────────────────────────────────────────

    fun addOrUpdateRequest(request: MessageRequest) {
        val current = _requests.value.toMutableList()
        val existing = current.indexOfFirst { it.senderId == request.senderId }
        if (existing >= 0) {
            // Update count and preview, keep original timestamp
            current[existing] = current[existing].copy(
                previewText  = request.previewText,
                messageCount = request.messageCount,
                timestamp    = request.timestamp
            )
        } else {
            current.add(request)
        }
        _requests.value = current.sortedByDescending { it.timestamp }
        persist(current)
    }

    fun deleteRequest(senderId: String) {
        val current = _requests.value.filter { it.senderId != senderId }
        _requests.value = current
        persist(current)
    }

    fun clearAll() {
        _requests.value = emptyList()
        prefs.edit().clear().apply()
    }

    // ── Serialization ─────────────────────────────────────────────────

    private fun loadRequests(): List<MessageRequest> {
        val json = prefs.getString(KEY_REQUESTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val obj = arr.getJSONObject(i)
                    MessageRequest(
                        senderId        = obj.getString("senderId"),
                        senderShortId   = obj.getString("senderShortId"),
                        senderPublicKey = obj.getString("senderPublicKey"),
                        previewText     = obj.getString("previewText"),
                        timestamp       = obj.getLong("timestamp"),
                        messageCount    = obj.optInt("messageCount", 1)
                    )
                }.getOrNull()
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun persist(requests: List<MessageRequest>) {
        val arr = JSONArray()
        requests.forEach { req ->
            arr.put(JSONObject().apply {
                put("senderId",        req.senderId)
                put("senderShortId",   req.senderShortId)
                put("senderPublicKey", req.senderPublicKey)
                put("previewText",     req.previewText)
                put("timestamp",       req.timestamp)
                put("messageCount",    req.messageCount)
            })
        }
        prefs.edit().putString(KEY_REQUESTS, arr.toString()).apply()
    }
}
