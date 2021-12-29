package com.trifork.timandroid.biometric

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.trifork.timandroid.models.errors.TIMBiometricError
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.crypto.Cipher
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//TODO Make this private
object TIMBiometric {
    fun presentBiometricPrompt(scope: CoroutineScope, fragment: Fragment, cipher: Cipher): Deferred<TIMResult<Cipher, TIMBiometricError>> = scope.async {
        suspendCoroutine { continuation ->
            BiometricUtil.showBiometricPrompt(
                fragment = fragment,
                listener = object : BiometricAuthListener {
                    override fun onBiometricAuthenticationSuccess(result: BiometricPrompt.AuthenticationResult) {
                        continuation.resume(
                            handleBiometricAuthenticationCallback(result.cryptoObject?.cipher, TIMBiometricError.BiometricAuthenticationError(null, Throwable("No cipher returned")))
                        )
                    }

                    override fun onBiometricAuthenticationError(error: TIMBiometricError.BiometricAuthenticationError) {
                        continuation.resume(
                            error.toTIMFailure()
                        )
                    }
                },
                cryptoObject = BiometricPrompt.CryptoObject(cipher)
            )
        }
    }

    private fun <T> handleBiometricAuthenticationCallback(value: T?, fallbackError: TIMBiometricError): TIMResult<T, TIMBiometricError> =
        when {
            value != null -> value.toTIMSuccess()
            else -> fallbackError.toTIMFailure()
        }
}