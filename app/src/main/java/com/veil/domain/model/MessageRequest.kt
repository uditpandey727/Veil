package com.veil.domain.model

/**
 * A message request from an unknown sender.
 *
 * When User A messages User B but B hasn't saved A as a contact,
 * the messages land here instead of the main inbox.
 *
 * User B can:
 *   - Accept → A becomes a contact, messages move to main inbox
 *   - Decline → messages deleted, sender never notified
 */
data class MessageRequest(
    val senderId       : String,
    val senderShortId  : String,     // "# A3F8C21B"
    val senderPublicKey: String,     // so we can add them as contact on accept
    val previewText    : String,     // first decrypted message (or 🔒 if failed)
    val timestamp      : Long,
    val messageCount   : Int = 1
)
