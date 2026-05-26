package com.veil.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.veil.data.repository.VeilRepository
import com.veil.ui.appearance.AppearancePreferences
import com.veil.ui.appearance.AppearanceViewModel
import com.veil.ui.chat.ChatViewModel
import com.veil.ui.home.HomeViewModel
import com.veil.ui.onboarding.OnboardingViewModel
import com.veil.ui.qr.QrViewModel
import com.veil.ui.requests.MessageRequestsViewModel
import com.veil.ui.settings.SettingsViewModel

class VeilViewModelFactory(
    private val repository : VeilRepository,
    private val appearance : AppearancePreferences,
    private val context    : Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
            OnboardingViewModel(repository) as T
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(repository) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(repository) as T
        modelClass.isAssignableFrom(QrViewModel::class.java) ->
            QrViewModel(repository) as T
        modelClass.isAssignableFrom(AppearanceViewModel::class.java) ->
            AppearanceViewModel(appearance, context) as T
        modelClass.isAssignableFrom(MessageRequestsViewModel::class.java) ->
            MessageRequestsViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}

class ChatViewModelFactory(
    private val repository: VeilRepository,
    private val contactId : String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository, contactId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
