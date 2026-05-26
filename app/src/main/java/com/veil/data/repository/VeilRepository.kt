package com.veil.data.repository

import android.util.Log
import com.veil.data.local.ContactsLocalDataSource
import com.veil.data.local.ConversationLocalDataSource
import com.veil.data.local.MessageRequestStore
import com.veil.data.local.SentMessageStore
import com.veil.data.local.UserLocalDataSource
import com.veil.data.remote.FirebaseDataSource
import com.veil.domain.model.Contact
import com.veil.domain.model.Conversation
import com.veil.domain.model.Message
import com.veil.domain.model.MessageFirebase
import com.veil.domain.model.MessageRequest
import com.veil.domain.model.MessageStatus
import com.veil.domain.model.User
import com.veil.domain.model.UserPublicProfile
import com.veil.security.VeilCrypto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Base64
import java.util.UUID

class VeilRepository(
    private val local        : UserLocalDataSource,
    private val contactsStore: ContactsLocalDataSource,
    private val convStore    : ConversationLocalDataSource,
    private val sentStore    : SentMessageStore,
    private val requestStore : MessageRequestStore,
    private val remote       : FirebaseDataSource,
    private val crypto       : VeilCrypto
) {
    companion object {
        private const val TAG = "VeilRepository"
    }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Tracks which chat is currently open.
     * When a message arrives for an active chat, we DON'T increment
     * the unread count — the user is already reading it.
     * Set by ChatViewModel.openChat() / closeChat().
     */
    private var activeChatContactId: String? = null

    fun openChat(contactId: String) {
        activeChatContactId = contactId
        // Immediately clear unread count when chat opens
        convStore.markAsRead(contactId)
        Log.d(TAG, "Chat opened for ${contactId.take(8)}, unread cleared")
    }

    fun closeChat(contactId: String) {
        if (activeChatContactId == contactId) {
            activeChatContactId = null
            Log.d(TAG, "Chat closed for ${contactId.take(8)}")
        }
    }

    // ── Identity ──────────────────────────────────────────────────────

    suspend fun setupNewIdentity(): Result<User> = runCatching {
        val user = local.getOrCreateUser()
        if (!local.hasKeyPair()) {
            val keyPair   = crypto.generateKeyPair()
            val publicB64 = Base64.getEncoder().encodeToString(keyPair.public.encoded)
            val privB64   = Base64.getEncoder().encodeToString(keyPair.private.encoded)
            local.storeKeyPair(publicB64, privB64)
            remote.uploadUserPublicProfile(
                UserPublicProfile(userId = user.userId, publicKey = publicB64)
            )
            Log.d(TAG, "Identity created: ${user.userId.take(8)}")
        }
        user
    }

    fun getCurrentUser(): User    = local.getOrCreateUser()
    fun isFirstLaunch(): Boolean  = local.isFirstLaunch()
    fun getMyPublicKey(): String? = local.getPublicKey()

    fun getMyKeyFingerprint(): String? {
        val pk = local.getPublicKey() ?: return null
        return crypto.publicKeyFingerprint(Base64.getDecoder().decode(pk))
    }

    // ── Settings ──────────────────────────────────────────────────────

    fun setDisplayName(name: String?)      = local.setDisplayName(name)
    fun getDisplayName(): String?          = local.getDisplayName()
    fun setAppLock(enabled: Boolean)       = local.setAppLockEnabled(enabled)
    fun isAppLockEnabled(): Boolean        = local.isAppLockEnabled()
    fun setDefaultDisappear(s: Int?)       = local.setDefaultDisappearSeconds(s)
    fun getDefaultDisappearSeconds(): Int? = local.getDefaultDisappearSeconds()
    fun setReadReceipts(enabled: Boolean)  = local.setSendReadReceipts(enabled)

    // ── Contacts ──────────────────────────────────────────────────────

    fun isContact(userId: String): Boolean = contactsStore.isContact(userId)
    fun getContact(id: String): Contact?   = contactsStore.getContact(id)
    fun getAllContacts(): List<Contact>     = contactsStore.getAllContacts()
    fun addContact(contact: Contact)       = contactsStore.saveContact(contact)

    fun updateContactNickname(contactId: String, nickname: String?) =
        contactsStore.updateNickname(contactId, nickname)

    fun deleteContact(contactId: String) {
        contactsStore.deleteContact(contactId)
        convStore.deleteConversation(contactId)
    }

    suspend fun fetchAndSaveContact(contactId: String): Result<Contact> = runCatching {
        val profile = remote.getUserPublicProfile(contactId)
            ?: throw IllegalStateException(
                "User not found. Make sure the ID is correct and " +
                "they've opened Veil at least once."
            )
        if (profile.publicKey.isBlank()) throw IllegalStateException("User has no public key.")
        Contact(
            contactId  = profile.userId,
            publicKey  = profile.publicKey,
            isVerified = false,
            addedAt    = System.currentTimeMillis()
        )
    }

    // ── Message Requests ──────────────────────────────────────────────

    val messageRequests: StateFlow<List<MessageRequest>> = requestStore.requests

    init { startInboxListener() }

    private fun startInboxListener() {
        val myId = local.getUserId()
        if (myId.isNullOrBlank()) return

        repoScope.launch {
            try {
                remote.listenForIncomingNotifications(myId)
                    .catch { e -> Log.e(TAG, "Inbox flow error (non-fatal): ${e.message}") }
                    .collect { notifications ->
                        notifications.forEach { notification ->
                            val senderId = notification.senderId
                            if (senderId == myId) return@forEach
                            if (contactsStore.isContact(senderId)) return@forEach
                            if (!requestStore.hasRequest(senderId)) {
                                createMessageRequest(senderId)
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Inbox listener failed (non-fatal): ${e.message}")
            }
        }
    }

    private suspend fun createMessageRequest(senderId: String) {
        val myId            = local.getUserId() ?: return
        val privateKeyBytes = local.getPrivateKeyBytes()
        val senderProfile   = try {
            remote.getUserPublicProfile(senderId) ?: return
        } catch (e: Exception) { return }

        var previewText  = "🔒 Encrypted message"
        var messageCount = 0

        try {
            var collected = false
            remote.listenToMessages(myId, senderId)
                .catch { }
                .collect { msgs ->
                    if (!collected) {
                        collected    = true
                        messageCount = msgs.size
                        val first    = msgs.firstOrNull()
                        if (first != null && privateKeyBytes != null &&
                            first.encryptedContent.contains("::")) {
                            runCatching {
                                val parts      = first.encryptedContent.split("::", limit = 2)
                                val sessionKey = crypto.decryptSessionKey(parts[0], privateKeyBytes)
                                previewText    = crypto.decryptMessage(parts[1], first.iv, sessionKey)
                            }
                        }
                    }
                }
        } catch (_: Exception) { }

        requestStore.addOrUpdateRequest(
            MessageRequest(
                senderId        = senderId,
                senderShortId   = "# ${senderId.take(8).uppercase()}",
                senderPublicKey = senderProfile.publicKey,
                previewText     = previewText,
                timestamp       = System.currentTimeMillis(),
                messageCount    = messageCount
            )
        )
    }

    suspend fun acceptMessageRequest(senderId: String): Result<String> = runCatching {
        val request = requestStore.getRequest(senderId)
            ?: throw IllegalStateException("Request not found")
        contactsStore.saveContact(Contact(
            contactId  = senderId,
            publicKey  = request.senderPublicKey,
            isVerified = false,
            addedAt    = System.currentTimeMillis()
        ))
        convStore.updateLastMessage(
            contactId       = senderId,
            timestamp       = request.timestamp,
            wasMine         = false,
            incrementUnread = true
        )
        requestStore.deleteRequest(senderId)
        try { remote.removeInboxNotification(local.getUserId()!!, senderId) }
        catch (e: Exception) { Log.e(TAG, "Failed to remove inbox notification: ${e.message}") }
        senderId
    }

    suspend fun declineMessageRequest(senderId: String) {
        val myId = local.getUserId() ?: return
        requestStore.deleteRequest(senderId)
        try {
            remote.deleteConversation(myId, senderId)
            remote.removeInboxNotification(myId, senderId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up declined request: ${e.message}")
        }
    }

    // ── Conversations ─────────────────────────────────────────────────

    fun getConversationsFlow(): Flow<List<Conversation>> =
        convStore.updateTick.map { buildConversationList() }

    private fun buildConversationList(): List<Conversation> =
        contactsStore.getAllContacts().map { contact ->
            Conversation(
                contactId            = contact.contactId,
                displayName          = contact.nickname
                                       ?: "# ${contact.contactId.take(8).uppercase()}",
                lastMessagePreview   = "🔒 Encrypted message",
                lastMessageTimestamp = convStore.getLastTimestamp(contact.contactId),
                lastMessageWasMine   = convStore.wasLastMessageMine(contact.contactId),
                unreadCount          = convStore.getUnreadCount(contact.contactId),
                isVerified           = contact.isVerified
            )
        }.sortedByDescending { it.lastMessageTimestamp }

    // Kept for backwards compat but openChat() is now preferred
    fun markConversationRead(contactId: String) = convStore.markAsRead(contactId)

    // ── Messaging ─────────────────────────────────────────────────────

    suspend fun sendMessage(
        recipientId          : String,
        plaintext            : String,
        messageId            : String,
        disappearAfterSeconds: Int? = local.getDefaultDisappearSeconds()
    ): Result<Unit> = runCatching {
        val sender = local.getOrCreateUser()

        val recipientPublicKey =
            contactsStore.getContact(recipientId)?.publicKey
            ?: remote.getUserPublicProfile(recipientId)?.publicKey
            ?: throw IllegalStateException("Recipient not found")

        val recipientPkBytes    = Base64.getDecoder().decode(recipientPublicKey)
        val sessionKey          = crypto.generateSessionKey()
        val (ciphertext, iv)    = crypto.encryptMessage(plaintext, sessionKey)
        val encryptedSessionKey = crypto.encryptSessionKey(sessionKey, recipientPkBytes)

        remote.sendMessage(
            MessageFirebase(
                messageId        = messageId,
                senderId         = sender.userId,
                receiverId       = recipientId,
                encryptedContent = "$encryptedSessionKey::$ciphertext",
                iv               = iv,
                timestamp        = System.currentTimeMillis(),
                expiresAt        = disappearAfterSeconds?.let {
                    System.currentTimeMillis() + (it * 1000L)
                }
            )
        )
        remote.writeInboxNotification(sender.userId, recipientId)
        sentStore.saveSentPlaintext(messageId, plaintext)
        convStore.updateLastMessage(
            contactId       = recipientId,
            timestamp       = System.currentTimeMillis(),
            wasMine         = true,
            incrementUnread = false
        )
    }

    fun getMessages(otherUserId: String): Flow<List<Message>> {
        val myId            = local.getUserId() ?: return flowOf(emptyList())
        val privateKeyBytes = local.getPrivateKeyBytes()

        return remote.listenToMessages(myId, otherUserId)
            .catch { e ->
                Log.e(TAG, "getMessages flow error: ${e.message}")
                emit(emptyList())
            }
            .map { firebaseMessages ->
                val hasNewFromThem = firebaseMessages.any { it.senderId == otherUserId }
                if (hasNewFromThem) {
                    // Only increment unread if this chat is NOT currently open
                    val isActiveChatOpen = activeChatContactId == otherUserId
                    convStore.updateLastMessage(
                        contactId       = otherUserId,
                        timestamp       = firebaseMessages.maxOfOrNull { it.timestamp }
                                          ?: System.currentTimeMillis(),
                        wasMine         = false,
                        incrementUnread = !isActiveChatOpen  // ← KEY FIX
                    )
                    // If chat IS open, keep count at 0
                    if (isActiveChatOpen) {
                        convStore.markAsRead(otherUserId)
                    }
                }
                firebaseMessages.map { fbMsg ->
                    val isFromMe = fbMsg.senderId == myId
                    val isForMe  = fbMsg.receiverId == myId
                    val plaintext: String? = when {
                        isFromMe -> sentStore.getSentPlaintext(fbMsg.messageId)
                        isForMe && privateKeyBytes != null &&
                        fbMsg.encryptedContent.contains("::") -> runCatching {
                            val parts      = fbMsg.encryptedContent.split("::", limit = 2)
                            val sessionKey = crypto.decryptSessionKey(parts[0], privateKeyBytes)
                            crypto.decryptMessage(parts[1], fbMsg.iv, sessionKey)
                        }.getOrNull()
                        else -> null
                    }
                    Message(
                        messageId        = fbMsg.messageId,
                        senderId         = fbMsg.senderId,
                        receiverId       = fbMsg.receiverId,
                        encryptedContent = fbMsg.encryptedContent,
                        iv               = fbMsg.iv,
                        timestamp        = fbMsg.timestamp,
                        plaintext        = plaintext,
                        status           = MessageStatus.DELIVERED,
                        expiresAt        = fbMsg.expiresAt
                    )
                }
            }
    }

    suspend fun deleteMessageFromServer(
        senderId: String, receiverId: String, messageId: String
    ) {
        sentStore.deleteSentPlaintext(messageId)
        remote.deleteMessage(senderId, receiverId, messageId)
    }

    fun wipeLocalData() {
        contactsStore.deleteAllContacts()
        convStore.clearAll()
        sentStore.clearAll()
        requestStore.clearAll()
        local.wipeAllData()
    }
}
