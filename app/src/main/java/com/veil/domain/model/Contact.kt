package com.veil.domain.model

data class Contact(
    val contactId  : String  = "",
    val nickname   : String? = null,      // local only, never synced
    val publicKey  : String  = "",
    val addedAt    : Long    = System.currentTimeMillis(),
    val isVerified : Boolean = false      // true if added via QR scan
)
