package com.trifork.timandroid.biometric

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.trifork.timandroid.TIM
import com.trifork.timandroid.models.errors.TIMStorageError
import javax.crypto.Cipher

class TIMBiometricData private constructor(
    private var title: String,
    private var subtitle: String,
    private var description: String,
    private var negativeButtonText: String,
    private var confirmationRequired: Boolean
) {
    private constructor(biometricUtilBuilder: Builder) : this(
        biometricUtilBuilder.title,
        biometricUtilBuilder.subtitle,
        biometricUtilBuilder.description,
        biometricUtilBuilder.negativeButtonText,
        biometricUtilBuilder.confirmationRequired
    )

    class Builder {
        var title: String = "Biometric login for my app"
            private set
        var subtitle: String = "Log in using your biometric credential"
            private set
        var description: String = "Input your Fingerprint or FaceID to ensure it's you!"
            private set
        var negativeButtonText: String = "Cancel"
            private set
        var confirmationRequired: Boolean = false
            private set

        fun title(title: String) = apply { this.title = title }
        fun subtitle(subtitle: String) = apply { this.subtitle = subtitle }
        fun description(description: String) = apply { this.description = description }
        fun negativeButtonText(negativeButtonText: String) = apply { this.negativeButtonText = negativeButtonText }
        fun setConfirmationRequired(confirmationRequired: Boolean) = apply { this.confirmationRequired = confirmationRequired }

        fun build() = TIMBiometricData(this)
    }

    fun showBiometricPrompt(
        fragment: Fragment,
        listener: BiometricAuthListener,
        cipher: Cipher
    ) {
        BiometricUtil.showBiometricPrompt(
            title,
            subtitle,
            description,
            negativeButtonText,
            confirmationRequired,
            fragment,
            listener,
            cipher
        )
    }
}

internal object BiometricUtil {

    private const val TAG = "BiometricUtil"

    /**
     * Displays a BiometricPrompt with provided configurations
     * @param title the desired title in the BiometricPrompt
     * @param subtitle the desired subtitle in the BiometricPrompt
     * @param description the desired description in the BiometricPrompt
     * @param negativeButtonText the desired negativeButtonText in the BiometricPrompt
     * @param confirmationRequired determines
     * @param fragment view used to display the custom tabs
     * @param listener [BiometricAuthListener] the callback style interface with results from the showed custom tab
     * @param cipher the cipher parsed to the biometric prompt, used to create the resulting cipher for encryption and decryption
     */
    fun showBiometricPrompt(
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String,
        confirmationRequired: Boolean,
        fragment: Fragment,
        listener: BiometricAuthListener,
        cipher: Cipher,
    ) {
        // Prepare BiometricPrompt Dialog
        val promptInfo = setBiometricPromptInfo(
            title,
            subtitle,
            description,
            negativeButtonText,
            confirmationRequired
        )

        // Attach with caller and callback handler
        val biometricPrompt = initBiometricPrompt(fragment, listener)

        // Authenticate with a CryptoObject
        biometricPrompt.apply {
            authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    //region Utility functions
    /**
     * Checks if Biometric Authentication is ready for the device e.g. th capability equals [BiometricManager.BIOMETRIC_SUCCESS]
     */
    //TODO Should we try and use biometric in case of BiometricManager.BIOMETRIC_STATUS_UNKNOWN?
    fun isBiometricReady(context: Context) = hasBiometricCapability(context) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Get the biometric capability AuthenticationStatus from [BiometricManager]
     */
    fun hasBiometricCapability(context: Context): Int = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)

    /**
     * Create a intent that can be used to display a [Settings.ACTION_SECURITY_SETTINGS], the device settings screen for biometric setup
     * @return a [Intent] the can be used with [startActivityForResult] to resume biometric login flow after changing biometric auth settings
     */
    fun launchBiometricSettings() = Intent(Settings.ACTION_SECURITY_SETTINGS)

    /**
     * Create a intent that can be used to display a [Settings.ACTION_BIOMETRIC_ENROLL] action, a screen for configuration of biometric enrolment
     * @return a [Intent] the can be used with [startActivityForResult] to resume biometric login flow after the user has enrolled biometric auth
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun configureBiometricEnrollmentIntent() = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
        putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
    }
    //endregion

    //region BiometricPrompt helpers
    /**
     * Initialize the biometric prompt with parsed [Fragment] and [BiometricAuthListener]
     * @param fragment [Fragment] the view used to show the custom tab
     * @param listener [BiometricAuthListener] the callback style interface with results from the showed custom tab
     * @return [BiometricPrompt] the initialized prompt that can be presented to the user
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
                listener.onBiometricAuthenticationError(TIMStorageError.BiometricAuthenticationError(errorCode, Throwable(errString.toString())))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                //This is called when the biometric authentication fails due to the user placing wrong finger on sensor, system cant read it, system cant detect face, etc.
                //So this is "soft-failures", not because something crashed/is wrong
                TIM.logger?.log(Log.DEBUG, TAG, "Authentication with biometric soft-failed")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                listener.onBiometricAuthenticationSuccess(result)
            }
        }

        return BiometricPrompt(fragment, executor, callback)
    }

    /**
     * Util function for calling the [BiometricPrompt.PromptInfo] builder
     */
    private fun setBiometricPromptInfo(
        title: String,
        subtitle: String,
        description: String,
        negativeButtonText: String,
        confirmationRequired: Boolean
    ): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(confirmationRequired)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
    }

    //endregion
}