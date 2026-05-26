package com.veil.di

import android.content.Context
import com.veil.data.local.ContactsLocalDataSource
import com.veil.data.local.ConversationLocalDataSource
import com.veil.data.local.MessageRequestStore
import com.veil.data.local.SentMessageStore
import com.veil.data.local.UserLocalDataSource
import com.veil.data.remote.FirebaseDataSource
import com.veil.data.repository.VeilRepository
import com.veil.security.VeilCrypto
import com.veil.ui.appearance.AppearancePreferences

class AppContainer(val context: Context) {
    val crypto        = VeilCrypto(context)
    val local         = UserLocalDataSource(context)
    val contacts      = ContactsLocalDataSource(context)
    val conversations = ConversationLocalDataSource(context)
    val sentMessages  = SentMessageStore(context)
    val requests      = MessageRequestStore(context)
    val remote        = FirebaseDataSource()
    val appearance    = AppearancePreferences(context)
    val repository    = VeilRepository(
        local, contacts, conversations, sentMessages, requests, remote, crypto
    )
}
