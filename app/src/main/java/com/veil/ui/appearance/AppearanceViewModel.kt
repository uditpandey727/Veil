package com.veil.ui.appearance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class AppearanceViewModel(
    private val prefs  : AppearancePreferences,
    private val context: Context
) : ViewModel() {

    data class UiState(
        val pendingIcon        : AppIcon? = null,   // icon waiting for confirmation
        val showIconConfirm    : Boolean  = false,
        val iconChangedSuccess : Boolean  = false   // shows brief success snackbar
    )

    val config: StateFlow<AppearanceConfig> = prefs.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, prefs.config.value)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ── Theme / colors — instant, no confirm needed ───────────────────

    fun setTheme(theme: AppTheme)          = prefs.setTheme(theme)
    fun setAccentColor(color: AccentColor) = prefs.setAccentColor(color)
    fun setBubbleColor(color: BubbleColor) = prefs.setBubbleColor(color)
    fun resetToDefaults() {
        prefs.reset()
        // Also reset icon to default
        AppIconManager.setIcon(context, AppIcon.DEFAULT)
    }

    // ── App icon — requires confirmation (briefly removes from launcher) ──

    /**
     * User tapped an icon — show confirmation dialog first.
     * Switching icons briefly removes the app from the launcher,
     * so we warn the user before doing it.
     */
    fun requestIconChange(icon: AppIcon) {
        if (icon == config.value.appIcon) return  // already active
        _uiState.update { it.copy(pendingIcon = icon, showIconConfirm = true) }
    }

    fun confirmIconChange() {
        val icon = _uiState.value.pendingIcon ?: return
        dismissIconConfirm()

        // 1. Save preference
        prefs.setAppIcon(icon)

        // 2. Switch the actual launcher icon via PackageManager
        AppIconManager.setIcon(context, icon)

        // 3. Brief success feedback
        _uiState.update { it.copy(iconChangedSuccess = true) }
    }

    fun dismissIconConfirm() {
        _uiState.update { it.copy(showIconConfirm = false, pendingIcon = null) }
    }

    fun clearIconChangedSuccess() {
        _uiState.update { it.copy(iconChangedSuccess = false) }
    }
}
