package com.veil.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veil.data.repository.VeilRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: VeilRepository
) : ViewModel() {

    data class UiState(
        // Identity
        val myUserId           : String  = "",
        val myShortId          : String  = "",
        val displayName        : String  = "",
        val keyFingerprint     : String  = "",

        // Privacy toggles
        val appLockEnabled     : Boolean = false,
        val sendReadReceipts   : Boolean = true,
        val showLastSeen       : Boolean = true,

        // Disappearing messages default
        val defaultDisappear   : Int?    = null,  // null = off

        // UI flags
        val showWipeConfirm    : Boolean = false,
        val showFingerprintInfo: Boolean = false,
        val copiedToClipboard  : Boolean = false,
        val wiped              : Boolean = false, // triggers nav back to onboarding
        val editingName        : Boolean = false,
        val nameInput          : String  = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() {
        val user        = repository.getCurrentUser()
        val fingerprint = repository.getMyKeyFingerprint() ?: "Generating…"
        val name        = repository.getDisplayName() ?: ""

        _uiState.update { it.copy(
            myUserId         = user.userId,
            myShortId        = "# ${user.userId.take(8).uppercase()}",
            displayName      = name,
            nameInput        = name,
            keyFingerprint   = fingerprint,
            appLockEnabled   = repository.isAppLockEnabled(),
            defaultDisappear = repository.getDefaultDisappearSeconds()
        )}
    }

    // ── Identity ──────────────────────────────────────────────────────

    fun startEditingName()             { _uiState.update { it.copy(editingName = true) } }
    fun onNameInputChange(v: String)   { _uiState.update { it.copy(nameInput = v) } }

    fun saveDisplayName() {
        val name = _uiState.value.nameInput.trim()
        repository.setDisplayName(name.ifBlank { null })
        _uiState.update { it.copy(displayName = name, editingName = false) }
    }

    fun cancelEditingName() {
        _uiState.update { it.copy(editingName = false, nameInput = it.displayName) }
    }

    fun onIdCopied() {
        viewModelScope.launch {
            _uiState.update { it.copy(copiedToClipboard = true) }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(copiedToClipboard = false) }
        }
    }

    // ── Privacy toggles ───────────────────────────────────────────────

    fun setAppLock(enabled: Boolean) {
        repository.setAppLock(enabled)
        _uiState.update { it.copy(appLockEnabled = enabled) }
    }

    fun setReadReceipts(enabled: Boolean) {
        repository.setReadReceipts(enabled)
        _uiState.update { it.copy(sendReadReceipts = enabled) }
    }

    fun setShowLastSeen(enabled: Boolean) {
        _uiState.update { it.copy(showLastSeen = enabled) }
    }

    fun setDefaultDisappear(seconds: Int?) {
        repository.setDefaultDisappear(seconds)
        _uiState.update { it.copy(defaultDisappear = seconds) }
    }

    // ── Fingerprint info dialog ────────────────────────────────────────

    fun showFingerprintInfo()    { _uiState.update { it.copy(showFingerprintInfo = true) } }
    fun dismissFingerprintInfo() { _uiState.update { it.copy(showFingerprintInfo = false) } }

    // ── Wipe data ─────────────────────────────────────────────────────

    fun showWipeConfirm()    { _uiState.update { it.copy(showWipeConfirm = true) } }
    fun dismissWipeConfirm() { _uiState.update { it.copy(showWipeConfirm = false) } }

    fun wipeAllData() {
        repository.wipeLocalData()
        _uiState.update { it.copy(showWipeConfirm = false, wiped = true) }
    }
}
