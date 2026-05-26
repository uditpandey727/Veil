package com.veil.ui.appearance

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AppearancePreferences
 *
 * Stores and exposes all visual customisation settings.
 * Backed by EncryptedSharedPreferences (consistent with the rest of the app).
 *
 * Exposes a StateFlow<AppearanceConfig> so any composable can react to changes.
 */
class AppearancePreferences(context: Context) {

    companion object {
        private const val PREFS_FILE   = "veil_appearance"
        private const val KEY_THEME    = "theme"
        private const val KEY_ACCENT   = "accent_hex"
        private const val KEY_BUBBLE   = "bubble_hex"
        private const val KEY_ICON     = "app_icon"

        // Defaults
        val DEFAULT_ACCENT = AccentColor.TEAL
        val DEFAULT_BUBBLE = BubbleColor.TEAL
        val DEFAULT_THEME  = AppTheme.DARK
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AppearanceConfig> = _config.asStateFlow()

    // ── Read ──────────────────────────────────────────────────────────

    private fun loadConfig() = AppearanceConfig(
        theme       = AppTheme.fromKey(prefs.getString(KEY_THEME, DEFAULT_THEME.key)!!),
        accentColor = AccentColor.fromHex(prefs.getString(KEY_ACCENT, DEFAULT_ACCENT.hex)!!),
        bubbleColor = BubbleColor.fromHex(prefs.getString(KEY_BUBBLE, DEFAULT_BUBBLE.hex)!!),
        appIcon     = AppIcon.fromKey(prefs.getString(KEY_ICON, AppIcon.DEFAULT.key)!!)
    )

    // ── Write ─────────────────────────────────────────────────────────

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.key).apply()
        _config.value = _config.value.copy(theme = theme)
    }

    fun setAccentColor(color: AccentColor) {
        prefs.edit().putString(KEY_ACCENT, color.hex).apply()
        _config.value = _config.value.copy(accentColor = color)
    }

    fun setBubbleColor(color: BubbleColor) {
        prefs.edit().putString(KEY_BUBBLE, color.hex).apply()
        _config.value = _config.value.copy(bubbleColor = color)
    }

    fun setAppIcon(icon: AppIcon) {
        prefs.edit().putString(KEY_ICON, icon.key).apply()
        _config.value = _config.value.copy(appIcon = icon)
    }

    fun reset() {
        prefs.edit().clear().apply()
        _config.value = AppearanceConfig()
    }
}

// ── Data models ───────────────────────────────────────────────────────

data class AppearanceConfig(
    val theme       : AppTheme    = AppearancePreferences.DEFAULT_THEME,
    val accentColor : AccentColor = AppearancePreferences.DEFAULT_ACCENT,
    val bubbleColor : BubbleColor = AppearancePreferences.DEFAULT_BUBBLE,
    val appIcon     : AppIcon     = AppIcon.DEFAULT
)

enum class AppTheme(val key: String, val label: String) {
    DARK        ("dark",    "Dark"),
    LIGHT       ("light",   "Light"),
    AMOLED      ("amoled",  "AMOLED Black"),
    SYSTEM      ("system",  "Follow system");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DARK
    }
}

enum class AccentColor(val hex: String, val label: String, val dark: String) {
    TEAL    ("#4ECDC4", "Teal",     "#2A8F88"),
    PURPLE  ("#9B7FE8", "Purple",   "#6B4FBB"),
    CORAL   ("#E87F7F", "Coral",    "#BB4F4F"),
    BLUE    ("#7FB5E8", "Blue",     "#4F85BB"),
    AMBER   ("#E8C37F", "Amber",    "#BB934F"),
    GREEN   ("#7FE8A0", "Green",    "#4FBB70"),
    PINK    ("#E87FBB", "Pink",     "#BB4F8B"),
    WHITE   ("#F0F0F2", "Monochrome","#C0C0C2");

    companion object {
        fun fromHex(hex: String) = entries.firstOrNull { it.hex == hex } ?: TEAL
    }
}

enum class BubbleColor(val hex: String, val label: String, val backgroundHex: String) {
    TEAL    ("#4ECDC4", "Teal",    "#1A3A38"),
    PURPLE  ("#9B7FE8", "Purple",  "#2A1A3A"),
    CORAL   ("#E87F7F", "Coral",   "#3A1A1A"),
    BLUE    ("#7FB5E8", "Blue",    "#1A2A3A"),
    AMBER   ("#E8C37F", "Amber",   "#3A2E1A"),
    GREEN   ("#7FE8A0", "Green",   "#1A3A24"),
    PINK    ("#E87FBB", "Pink",    "#3A1A2A"),
    GREY    ("#8A8A90", "Grey",    "#222226");

    companion object {
        fun fromHex(hex: String) = entries.firstOrNull { it.hex == hex } ?: TEAL
    }
}

enum class AppIcon(val key: String, val label: String, val emoji: String) {
    DEFAULT ("default", "Default",      "◈"),
    GHOST   ("ghost",   "Ghost",        "👻"),
    SHIELD  ("shield",  "Shield",       "🛡"),
    LOCK    ("lock",    "Lock",         "🔒"),
    MINIMAL ("minimal", "Minimal",      "▪");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
