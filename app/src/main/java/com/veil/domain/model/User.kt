package com.veil.domain.model

import java.util.UUID

data class User(
    val userId      : String = UUID.randomUUID().toString(),
    val displayName : String? = null,
    val publicKey   : String = "",
    val createdAt   : Long   = System.currentTimeMillis()
)

// What gets stored in Firebase — public info only
data class UserPublicProfile(
    val userId    : String = "",
    val publicKey : String = "",
    val lastSeen  : Long?  = null
)
