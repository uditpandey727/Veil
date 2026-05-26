package com.veil.domain.model

// Full message — used inside the app (plaintext in memory only)
data class Message(
    val messageId        : String        = "",
    val senderId         : String        = "",
    val receiverId       : String        = "",
    val encryptedContent : String        = "",
    val iv               : String        = "",
    val timestamp        : Long          = System.currentTimeMillis(),
    val plaintext        : String?       = null,   // in memory only, never persisted
    val status           : MessageStatus = MessageStatus.SENDING,
    val expiresAt        : Long?         = null,
    val isExpired        : Boolean       = false
)

// What gets written to / read from Firebase
data class MessageFirebase(
    val messageId        : String = "",
    val senderId         : String = "",
    val receiverId       : String = "",
    val encryptedContent : String = "",   // "encryptedSessionKey::ciphertext"
    val iv               : String = "",
    val timestamp        : Long   = 0L,
    val expiresAt        : Long?  = null
)

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}
