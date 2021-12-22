package com.trifork.timandroid.biometric

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.trifork.timandroid.models.errors.TIMBiometricError

object BiometricUtil {

    /**
     * Checks if Biometric Authentication is ready for the device
     */
    fun isBiometricReady(context: Context) = hasBiometricCapability(context) == BiometricManager.BIOMETRIC_SUCCESS

    //TODO Would we rather want to have a enrollment activity as per android documentation?
    /**
     * Navigates to Device Settings screen Biometric Setup
     */
    fun launchBiometricSettings(context: Context) {
        ActivityCompat.startActivity(
            context,
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            null
        )
    }

    /**
     *
     */
    private fun initBiometricPrompt(
        fragment: Fragment,
        listener: BiometricAuthListener
    ): BiometricPrompt {
        // Attach calling Activity
        val executor = ContextCompat.getMainExecutor(fragment.context)

        // Attach callback handlers
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                listener.onBiometricAuthenticationError(TIMBiometricError.BiometricAuthenticationError(Throwable(errString.toString())))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                listener.onBiometricAuthenticationError(TIMBiometricError.BiometricAuthenticationError(Throwable("Authentication failed for an unknown reason")))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                listener.onBiometricAuthenticationSuccess(result)
            }
        }

        return BiometricPrompt(fragment, executor, callback)
    }


    private fun setBiometricPromptInfo(
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String
    ) : BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    /**
     * Displays a BiometricPrompt with provided configurations
     */
    fun showBiometricPrompt(
        title: String = "Biometric login for my app",
        subtitle: String = "Log in using your biometric credential",
        description: String = "Input your Fingerprint or FaceID to ensure it's you!",
        negativeButtonText: String = "Use account password",
        fragment: Fragment,
        listener: BiometricAuthListener,
        cryptoObject: BiometricPrompt.CryptoObject,
    ) {
        // Prepare BiometricPrompt Dialog
        val promptInfo = setBiometricPromptInfo(
            title,
            subtitle,
            description,
            negativeButtonText
        )

        // Attach with caller and callback handler
        val biometricPrompt = initBiometricPrompt(fragment, listener)

        // Authenticate with a CryptoObject
        biometricPrompt.apply {
            authenticate(promptInfo, cryptoObject)
        }
    }

    private fun hasBiometricCapability(context: Context) : Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG)

        //TODO Do we want to have access to these in app? - JHE 21.12.21
        /*when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Log.d(TAG, "App can authenticate using biometrics.")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Log.e(TAG, "No biometric features available on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Log.e(TAG, "Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Log.d(TAG, "Biometric features none enrolled.")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                TODO()
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                TODO()
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                TODO()
            }
        }*/
    }

}