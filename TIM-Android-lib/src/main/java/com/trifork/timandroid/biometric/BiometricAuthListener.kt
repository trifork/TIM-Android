package com.trifork.timandroid.biometric

import androidx.biometric.BiometricPrompt
import com.trifork.timandroid.models.errors.TIMStorageError

public interface BiometricAuthListener {
    public fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult)
    public fun onBiometricAuthenticationError(error: TIMStorageError.BiometricAuthenticationError)
}