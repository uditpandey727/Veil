package com.veil.ui.appearance

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * AppIconManager
 *
 * Changes the launcher icon at runtime using Android's activity-alias mechanism.
 *
 * How it works:
 *   - Each icon variant has an <activity-alias> in AndroidManifest.xml
 *   - Only ONE alias is ENABLED at a time — that's the one Android shows in the launcher
 *   - When user picks a new icon:
 *       1. Enable the new alias
 *       2. Disable all other aliases
 *   - Android briefly removes the icon from the launcher then re-adds it
 *     (this is normal — no way around it, it's an Android limitation)
 *
 * Requirements:
 *   - Each icon needs its own mipmap drawable set in res/mipmap-
 *   - The alias android:name must match the componentName strings below
 *   - MainActivity must NOT have an intent-filter (the aliases handle that)
 *
 * Limitations:
 *   - Some launchers (e.g. MIUI, Samsung One UI) may not update immediately
 *   - The app shortcut briefly disappears during the switch (~1-2 seconds)
 *   - Cannot be tested on emulator (emulator doesn't have a real launcher)
 */
object AppIconManager {

    private const val TAG = "AppIconManager"
    private const val PACKAGE = "com.veil"

    /**
     * Maps each AppIcon to its activity-alias component name.
     * Must match android:name in AndroidManifest.xml exactly.
     */
    private val iconToAlias = mapOf(
        AppIcon.DEFAULT  to "$PACKAGE.icon.Default",
        AppIcon.GHOST    to "$PACKAGE.icon.Ghost",
        AppIcon.SHIELD   to "$PACKAGE.icon.Shield",
        AppIcon.LOCK     to "$PACKAGE.icon.Lock",
        AppIcon.MINIMAL  to "$PACKAGE.icon.Minimal"
    )

    /**
     * Switch the app icon to the given variant.
     *
     * Call this from AppearanceViewModel after saving to prefs.
     * The change takes effect after a brief launcher refresh (Android handles this).
     */
    fun setIcon(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        Log.d(TAG, "Switching app icon to: ${icon.label}")

        iconToAlias.forEach { (appIcon, aliasName) ->
            val component   = ComponentName(PACKAGE, aliasName)
            val shouldEnable = appIcon == icon

            val newState = if (shouldEnable)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            try {
                pm.setComponentEnabledSetting(
                    component,
                    newState,
                    PackageManager.DONT_KILL_APP   // don't restart the app
                )
                Log.d(TAG, "  ${appIcon.label}: ${if (shouldEnable) "ENABLED" else "disabled"}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set ${appIcon.label}: ${e.message}")
            }
        }

        Log.d(TAG, "Icon switch complete. Launcher will refresh shortly.")
    }

    /**
     * Returns which icon is currently active by checking component states.
     * Useful for restoring state on app restart.
     */
    fun getCurrentIcon(context: Context): AppIcon {
        val pm = context.packageManager

        return iconToAlias.entries.firstOrNull { (_, aliasName) ->
            val component = ComponentName(PACKAGE, aliasName)
            try {
                pm.getComponentEnabledSetting(component) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } catch (e: Exception) {
                false
            }
        }?.key ?: AppIcon.DEFAULT
    }
}
