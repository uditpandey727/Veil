package com.veil.ui.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veil.data.repository.VeilRepository
import com.veil.domain.model.MessageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessageRequestsViewModel(
    private val repository: VeilRepository
) : ViewModel() {

    data class UiState(
        val isProcessing    : Boolean = false,
        val acceptedContactId: String? = null,  // triggers nav to chat
        val declinedId      : String? = null,   // shows undo snackbar
        val errorMessage    : String? = null
    )

    // Live list from repository
    val requests: StateFlow<List<MessageRequest>> = repository.messageRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun accept(senderId: String) {
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            repository.acceptMessageRequest(senderId)
                .onSuccess { contactId ->
                    _uiState.update { it.copy(
                        isProcessing     = false,
                        acceptedContactId = contactId
                    )}
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        isProcessing = false,
                        errorMessage = "Failed to accept: ${e.message}"
                    )}
                }
        }
    }

    fun decline(senderId: String) {
        viewModelScope.launch {
            repository.declineMessageRequest(senderId)
            _uiState.update { it.copy(declinedId = senderId) }
        }
    }

    fun clearNavigation()   { _uiState.update { it.copy(acceptedContactId = null) } }
    fun clearDeclined()     { _uiState.update { it.copy(declinedId = null) } }
    fun clearError()        { _uiState.update { it.copy(errorMessage = null) } }
}
