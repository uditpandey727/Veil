package com.veil.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * BiometricHelper
 *
 * Wraps androidx.biometric for clean usage in MainActivity.
 * Supports fingerprint, face unlock, and PIN/pattern fallback.
 *
 * Usage:
 *   BiometricHelper.authenticate(
 *       activity  = this,
 *       onSuccess = { /* unlock app */ },
 *       onFailed  = { /* wrong biometric */ },
 *       onError   = { msg -> /* show error */ }
 *   )
 */
object BiometricHelper {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
               BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity : FragmentActivity,
        onSuccess: () -> Unit,
        onFailed : () -> Unit = {},
        onError  : (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationFailed() {
                onFailed()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User pressed back or cancelled — don't treat as error
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Veil")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
