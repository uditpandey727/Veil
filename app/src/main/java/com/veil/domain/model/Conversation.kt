package com.veil.domain.model

/**
 * A conversation summary shown on the home screen.
 * This is derived locally — never fetched as a whole from Firebase.
 *
 * Built by combining:
 *   - Contact (local, encrypted storage)
 *   - Latest message from Firebase (still encrypted — we decrypt locally)
 */
data class Conversation(
    val contactId: String,

    // Display name — local nickname if set, otherwise shortened ID
    // e.g. "Ghost" or "# A3F8C21B"
    val displayName: String,

    // Last message preview — always shown as locked if encrypted
    val lastMessagePreview: String,
    val lastMessageTimestamp: Long,
    val lastMessageWasMine: Boolean,

    // Unread badge
    val unreadCount: Int = 0,

    // Is the contact's key verified via QR?
    val isVerified: Boolean = false,

    // Message status of last message
    val lastMessageStatus: MessageStatus = MessageStatus.DELIVERED
)
