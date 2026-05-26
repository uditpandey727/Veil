package com.veil.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veil.data.repository.VeilRepository
import com.veil.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * OnboardingViewModel
 *
 * Handles first-launch identity creation.
 * No forms, no fields, no phone number — just generate an identity.
 */
class OnboardingViewModel(
    private val repository: VeilRepository
) : ViewModel() {

    sealed class UiState {
        object Idle       : UiState()
        object Loading    : UiState()
        data class Success(val user: User) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Called when user taps "Create Anonymous Identity".
     * Generates UUID + RSA keypair, uploads public key to Firebase.
     * No user input required.
     */
    fun createIdentity() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            repository.setupNewIdentity()
                .onSuccess { user ->
                    _uiState.value = UiState.Success(user)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(
                        error.message ?: "Something went wrong generating your identity"
                    )
                }
        }
    }

    /**
     * Optional: user can set a local display name after identity is created.
     * This is stored ONLY on device, never synced anywhere.
     */
    fun setDisplayName(name: String) {
        if (name.isNotBlank()) {
            repository.setDisplayName(name.trim())
        }
    }
}
