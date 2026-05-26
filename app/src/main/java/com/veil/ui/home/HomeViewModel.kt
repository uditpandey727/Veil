package com.veil.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veil.data.repository.VeilRepository
import com.veil.domain.model.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: VeilRepository
) : ViewModel() {

    data class UiState(
        val conversations    : List<Conversation> = emptyList(),
        val myDisplayName    : String?            = null,
        val myShortId        : String             = "",
        val isLoading        : Boolean            = true,
        val showAddContact   : Boolean            = false,
        val isAddingContact  : Boolean            = false,
        val addContactError  : String?            = null,
        val addContactSuccess: String?            = null,
        val requestCount     : Int                = 0   // badge on requests button
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadUser()
        observeConversationsAndRequests()
    }

    private fun loadUser() {
        val user = repository.getCurrentUser()
        _uiState.update { it.copy(
            myDisplayName = repository.getDisplayName(),
            myShortId     = "# ${user.userId.take(8).uppercase()}"
        )}
    }

    private fun observeConversationsAndRequests() {
        viewModelScope.launch {
            // Combine conversations flow + request count so both update the UI
            combine(
                repository.getConversationsFlow(),
                repository.messageRequests
            ) { conversations, requests ->
                Pair(conversations, requests.size)
            }.collect { (conversations, requestCount) ->
                _uiState.update { it.copy(
                    conversations = conversations,
                    requestCount  = requestCount,
                    isLoading     = false
                )}
            }
        }
    }

    // ── Add contact ───────────────────────────────────────────────────

    fun showAddContact() {
        _uiState.update { it.copy(
            showAddContact    = true,
            addContactError   = null,
            addContactSuccess = null
        )}
    }

    fun dismissAddContact() {
        _uiState.update { it.copy(
            showAddContact    = false,
            addContactError   = null,
            addContactSuccess = null,
            isAddingContact   = false
        )}
    }

    fun addContactById(rawInput: String) {
        val contactId = rawInput.trim().removePrefix("#").trim()

        if (contactId.isBlank()) {
            _uiState.update { it.copy(addContactError = "Please enter an ID.") }
            return
        }
        val myId = repository.getCurrentUser().userId
        if (contactId == myId || myId.startsWith(contactId, ignoreCase = true)) {
            _uiState.update { it.copy(addContactError = "That's your own ID.") }
            return
        }
        if (repository.isContact(contactId)) {
            _uiState.update { it.copy(addContactError = "Already in your contacts.") }
            return
        }

        _uiState.update { it.copy(isAddingContact = true, addContactError = null) }

        viewModelScope.launch {
            repository.fetchAndSaveContact(contactId)
                .onSuccess { contact ->
                    repository.addContact(contact)
                    _uiState.update { it.copy(
                        isAddingContact   = false,
                        showAddContact    = false,
                        addContactSuccess = contact.contactId,
                        addContactError   = null
                    )}
                }
                .onFailure { e ->
                    _uiState.update { it.copy(
                        isAddingContact = false,
                        addContactError = when {
                            e.message?.contains("not found", ignoreCase = true) == true ->
                                "No Veil user found with that ID.\nMake sure they've opened the app at least once."
                            else -> "Couldn't add contact: ${e.message}"
                        }
                    )}
                }
        }
    }

    fun clearAddContactSuccess() {
        _uiState.update { it.copy(addContactSuccess = null) }
    }
}
