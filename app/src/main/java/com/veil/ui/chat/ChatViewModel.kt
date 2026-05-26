package com.veil.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veil.data.repository.VeilRepository
import com.veil.domain.model.Message
import com.veil.domain.model.MessageStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: VeilRepository,
    val contactId: String
) : ViewModel() {

    companion object {
        private const val TAG = "VeilChat"
    }

    // Exposed so ChatScreen can identify which messages are mine
    val myUserId: String = repository.getCurrentUser().userId

    data class UiState(
        val messages           : List<Message> = emptyList(),
        val contactDisplayName : String        = "",
        val isContactVerified  : Boolean       = false,
        val isSending          : Boolean       = false,
        val disappearSeconds   : Int?          = null,
        val showMessageOptions : String?       = null,
        val errorMessage       : String?       = null,
        val isLoading          : Boolean       = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val optimisticMessages = mutableMapOf<String, Message>()
    private val expiryJobs         = mutableMapOf<String, Job>()
    private var messageListenerJob : Job? = null

    init {
        loadContact()
        startListeningToMessages()
    }

    private fun loadContact() {
        val contact = repository.getContact(contactId)
        _uiState.update { it.copy(
            contactDisplayName = contact?.nickname
                                 ?: "# ${contactId.take(8).uppercase()}",
            isContactVerified  = contact?.isVerified ?: false,
            disappearSeconds   = repository.getDefaultDisappearSeconds()
        )}
        // Tell repository this chat is now open —
        // clears unread count and prevents re-incrementing while reading
        repository.openChat(contactId)
    }

    private fun startListeningToMessages() {
        messageListenerJob?.cancel()
        messageListenerJob = viewModelScope.launch {
            try {
                repository.getMessages(contactId).collect { incomingMessages ->
                    Log.d(TAG, "Received ${incomingMessages.size} messages")
                    rebuildMessageList(incomingMessages)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listener error: ${e.message}")
                _uiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = "Connection error: ${e.message}"
                )}
            }
        }
    }

    private fun rebuildMessageList(firebaseMessages: List<Message>) {
        val firebaseIds = firebaseMessages.map { it.messageId }.toSet()

        val merged = buildList {
            firebaseMessages.forEach { fbMsg ->
                val optimistic = optimisticMessages[fbMsg.messageId]
                if (optimistic != null) {
                    add(optimistic.copy(status = MessageStatus.DELIVERED))
                    optimisticMessages.remove(fbMsg.messageId)
                } else {
                    add(fbMsg)
                }
            }
            optimisticMessages.values.forEach { pending ->
                if (pending.messageId !in firebaseIds) add(pending)
            }
        }.sortedBy { it.timestamp }

        _uiState.update { it.copy(messages = merged, isLoading = false) }
        merged.forEach { scheduleExpiryIfNeeded(it) }
    }

    // ── Sending ───────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        if (text.isBlank() || _uiState.value.isSending) return

        val messageId = UUID.randomUUID().toString()
        val optimistic = Message(
            messageId        = messageId,
            senderId         = myUserId,
            receiverId       = contactId,
            plaintext        = text,
            encryptedContent = "",
            iv               = "",
            status           = MessageStatus.SENDING,
            timestamp        = System.currentTimeMillis(),
            expiresAt        = _uiState.value.disappearSeconds?.let {
                System.currentTimeMillis() + (it * 1000L)
            }
        )

        optimisticMessages[messageId] = optimistic
        _uiState.update { it.copy(
            messages  = (it.messages + optimistic).sortedBy { m -> m.timestamp },
            isSending = true
        )}

        viewModelScope.launch {
            repository.sendMessage(
                recipientId          = contactId,
                plaintext            = text,
                messageId            = messageId,
                disappearAfterSeconds = _uiState.value.disappearSeconds
            ).onSuccess {
                optimisticMessages[messageId] = optimistic.copy(status = MessageStatus.SENT)
                updateMessageStatus(messageId, MessageStatus.SENT)
            }.onFailure { e ->
                optimisticMessages.remove(messageId)
                updateMessageStatus(messageId, MessageStatus.FAILED)
                _uiState.update { it.copy(errorMessage = "Send failed: ${e.message}") }
            }
            _uiState.update { it.copy(isSending = false) }
        }
    }

    private fun updateMessageStatus(messageId: String, status: MessageStatus) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.messageId == messageId) msg.copy(status = status) else msg
            })
        }
    }

    // ── Disappearing messages ─────────────────────────────────────────

    private fun scheduleExpiryIfNeeded(message: Message) {
        val expiresAt = message.expiresAt ?: return
        if (expiryJobs.containsKey(message.messageId)) return
        val ms = expiresAt - System.currentTimeMillis()
        if (ms <= 0) { removeMessageLocally(message.messageId); return }
        expiryJobs[message.messageId] = viewModelScope.launch {
            delay(ms)
            removeMessageLocally(message.messageId)
        }
    }

    private fun removeMessageLocally(messageId: String) {
        optimisticMessages.remove(messageId)
        _uiState.update { it.copy(
            messages = it.messages.filter { m -> m.messageId != messageId }
        )}
        expiryJobs.remove(messageId)
    }

    fun setDisappearTimer(seconds: Int?) {
        _uiState.update { it.copy(disappearSeconds = seconds) }
    }

    // ── Message options ───────────────────────────────────────────────

    fun showMessageOptions(messageId: String) {
        _uiState.update { it.copy(showMessageOptions = messageId) }
    }

    fun dismissMessageOptions() {
        _uiState.update { it.copy(showMessageOptions = null) }
    }

    fun deleteMessage(messageId: String) {
        val msg = _uiState.value.messages.find { it.messageId == messageId }
        removeMessageLocally(messageId)
        dismissMessageOptions()
        if (msg != null && msg.senderId == myUserId) {
            viewModelScope.launch {
                runCatching {
                    repository.deleteMessageFromServer(msg.senderId, msg.receiverId, messageId)
                }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    override fun onCleared() {
        super.onCleared()
        // Tell repository this chat is closed —
        // future messages for this contact will increment unread again
        repository.closeChat(contactId)
        expiryJobs.values.forEach { it.cancel() }
        messageListenerJob?.cancel()
    }
}
