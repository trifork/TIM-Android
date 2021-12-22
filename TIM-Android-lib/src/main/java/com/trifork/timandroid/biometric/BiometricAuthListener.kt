package com.trifork.timandroid.biometric

import androidx.biometric.BiometricPrompt
import com.trifork.timandroid.models.errors.TIMBiometricError

interface BiometricAuthListener {
    fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult)
    fun onBiometricAuthenticationError(error: TIMBiometricError.BiometricAuthenticationError)
}