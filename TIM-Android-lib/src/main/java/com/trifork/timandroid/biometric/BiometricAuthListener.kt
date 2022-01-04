package com.trifork.timandroid.biometric

import androidx.biometric.BiometricPrompt
import com.trifork.timandroid.models.errors.TIMStorageError

interface BiometricAuthListener {
    fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult)
    fun onBiometricAuthenticationError(error: TIMStorageError.BiometricAuthenticationError)
}