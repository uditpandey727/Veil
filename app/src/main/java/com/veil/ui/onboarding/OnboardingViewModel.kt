package com.veil.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veil.data.repository.VeilRepository
import com.veil.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: VeilRepository
) : ViewModel() {

    sealed class UiState {
        object Idle    : UiState()
        object Loading : UiState()
        data class Success(val user: User) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun createIdentity() {
        // Don't allow double-tap
        if (_uiState.value is UiState.Loading) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.setupNewIdentity()
                .onSuccess { user -> _uiState.value = UiState.Success(user) }
                .onFailure { e  -> _uiState.value = UiState.Error(e.message ?: "Unknown error") }
        }
    }

    fun setDisplayName(name: String) {
        if (name.isNotBlank()) repository.setDisplayName(name.trim())
    }
}
