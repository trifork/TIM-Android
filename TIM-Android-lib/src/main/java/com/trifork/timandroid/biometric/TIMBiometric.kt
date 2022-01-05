package com.trifork.timandroid.biometric

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.trifork.timandroid.models.errors.TIMStorageError
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal object TIMBiometric {
    fun presentBiometricPrompt(scope: CoroutineScope, timBiometricUtil: TIMBiometricUtil, fragment: Fragment, cipher: Cipher): Deferred<TIMResult<Cipher, TIMStorageError>> = scope.async {
        suspendCoroutine { continuation ->
            timBiometricUtil.showBiometricPrompt(
                fragment = fragment,
                listener = object : BiometricAuthListener {
                    override fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult) {
                        continuation.resume(
                            handleBiometricAuthenticationCallback(result.cryptoObject?.cipher, TIMStorageError.BiometricAuthenticationError(null, Throwable("No cipher returned")))
                        )
                    }

                    override fun onBiometricAuthenticationError(error: TIMStorageError.BiometricAuthenticationError) {
                        continuation.resume(
                            error.toTIMFailure()
                        )
                    }
                },
                cipher = cipher
            )
        }
    }

    private fun <T> handleBiometricAuthenticationCallback(value: T?, fallbackError: TIMStorageError): TIMResult<T, TIMStorageError> =
        when {
            value != null -> value.toTIMSuccess()
            else -> fallbackError.toTIMFailure()
        }
}