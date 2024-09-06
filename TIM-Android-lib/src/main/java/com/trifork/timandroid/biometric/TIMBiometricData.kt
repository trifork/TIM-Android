package com.trifork.timandroid.biometric

import android.app.*
import android.content.*
import android.os.*
import android.provider.*
import android.util.Log.*
import androidx.annotation.*
import androidx.biometric.*
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.core.content.*
import androidx.fragment.app.*
import com.trifork.timandroid.*
import com.trifork.timandroid.models.errors.*
import javax.crypto.*

public class TIMBiometricData private constructor(
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

    public class Builder {

        public var title: String = "Biometric login for my app"
            private set

        public var subtitle: String = "Log in using your biometric credential"
            private set

        public var description: String = "Input your Fingerprint or FaceID to ensure it's you!"
            private set

        public var negativeButtonText: String = "Cancel"
            private set

        public var confirmationRequired: Boolean = false
            private set

        public fun title(title: String): Builder = apply {
            this.title = title
        }

        public fun subtitle(subtitle: String): Builder = apply {
            this.subtitle = subtitle
        }

        public fun description(description: String): Builder = apply {
            this.description = description
        }

        public fun negativeButtonText(negativeButtonText: String): Builder = apply {
            this.negativeButtonText = negativeButtonText
        }

        public fun setConfirmationRequired(confirmationRequired: Boolean): Builder = apply {
            this.confirmationRequired = confirmationRequired
        }

        public fun build(): TIMBiometricData = TIMBiometricData(this)
    }

    public fun showBiometricPrompt(
        fragmentActivity: FragmentActivity,
        listener: BiometricAuthListener,
        cipher: Cipher
    ) {
        BiometricUtil.showBiometricPrompt(
            title = title,
            subtitle = subtitle,
            description = description,
            negativeButtonText = negativeButtonText,
            confirmationRequired = confirmationRequired,
            fragmentActivity = fragmentActivity,
            listener = listener,
            cipher = cipher
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
        fragmentActivity: FragmentActivity,
        listener: BiometricAuthListener,
        cipher: Cipher,
    ) {
        val promptInfo: BiometricPrompt.PromptInfo = setBiometricPromptInfo(
            title = title,
            subtitle = subtitle,
            description = description,
            negativeButtonText = negativeButtonText,
            confirmationRequired = confirmationRequired
        )

        TIM.logger?.log(priority = DEBUG, tag = TAG, msg = "Created BiometricPromptInfo")

        val biometricPrompt: BiometricPrompt = initBiometricPrompt(fragmentActivity, listener)

        biometricPrompt.authenticate(
            /* info = */ promptInfo,
            /* crypto = */ BiometricPrompt.CryptoObject(cipher)
        )

        TIM.logger?.log(priority = DEBUG, tag = TAG, msg = "Presented BiometricPrompt")
    }

    //region Utility functions
    /**
     * Checks if Biometric Authentication is ready for the device e.g. th capability equals [BiometricManager.BIOMETRIC_SUCCESS]
     */
    //TODO Should we try and use biometric in case of BiometricManager.BIOMETRIC_STATUS_UNKNOWN?
    fun isBiometricReady(context: Context): Boolean =
        hasBiometricCapability(context) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Get the biometric capability AuthenticationStatus from [BiometricManager]
     */
    fun hasBiometricCapability(context: Context): TIMBiometricAuthentication =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)

    /**
     * Create a intent that can be used to display a [Settings.ACTION_SECURITY_SETTINGS], the device settings screen for biometric setup
     * @return a [Intent] the can be used with [Activity.startActivityForResult] to resume biometric login flow after changing biometric auth settings
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
     * @param fragmentActivity [FragmentActivity] the activity used to show the custom tab
     * @param listener [BiometricAuthListener] the callback style interface with results from the showed custom tab
     * @return [BiometricPrompt] the initialized prompt that can be presented to the user
     */
    private fun initBiometricPrompt(
        activity: FragmentActivity,
        listener: BiometricAuthListener
    ): BiometricPrompt {
        // Attach calling Activity
        val executor = ContextCompat.getMainExecutor(activity)

        // Attach callback handlers
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                listener.onBiometricAuthenticationError(
                    TIMStorageError.BiometricAuthenticationError(
                        errorCode,
                        Throwable(errString.toString())
                    )
                )
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                //This is called when the biometric authentication fails due to the user placing wrong finger on sensor, system cant read it, system cant detect face, etc.
                //So this is "soft-failures", not because something crashed/is wrong
                TIM.logger?.log(DEBUG, TAG, "Authentication with biometric soft-failed")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                listener.onBiometricAuthenticationSuccess(result)
            }
        }
        return BiometricPrompt(activity, executor, callback)
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