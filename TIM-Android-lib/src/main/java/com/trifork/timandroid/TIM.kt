package com.trifork.timandroid

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import com.trifork.timandroid.appauth.AppAuthController
import com.trifork.timandroid.biometric.BiometricUtil
import com.trifork.timandroid.biometric.TIMBiometricData
import com.trifork.timandroid.helpers.TIMEncryptedStorageLoggerInternal
import com.trifork.timandroid.helpers.TIMLogger
import com.trifork.timandroid.helpers.TIMLoggerInternal
import com.trifork.timandroid.internal.TIMAppBackgroundMonitorInternal
import com.trifork.timandroid.internal.TIMAuthInternal
import com.trifork.timandroid.internal.TIMDataStorageInternal
import com.trifork.timandroid.models.TIMConfiguration
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.keyservice.TIMKeyServiceImpl
import com.trifork.timencryptedstorage.securestorage.TIMEncryptedSharedPreferences

// TODO: In the README.md it should contain a section about the weird quirk between redirectUri in BuildConfig and in Manifest ("app:/" vs "app") - MFJ (20/09/2021)
object TIM {

    private var _storage: TIMDataStorage? = null
    val storage: TIMDataStorage
        @Throws(RuntimeException::class)
        get() =
            _storage ?: throw RuntimeException("Accessing TIM.storage before calling TIM.configure(...) is not allowed!")

    private var _auth: TIMAuth? = null
    val auth: TIMAuth
        @Throws(RuntimeException::class)
        get() = _auth
            ?: throw RuntimeException("Accessing TIM.auth before calling TIM.configure(...) is not allowed!")

    private var _logger: TIMLogger? = null
    val logger: TIMLogger?
        get() = _logger

    /**
     * Indicates whether [TIM] has been configure by a call to [configure]
     */
    val isConfigured
        get() = _auth != null && _storage != null

    // TODO: Consider weak reference here - MFJ (14/09/2021)
    /**
     * @param config TIMConfiguration
     * @param context Context
     * @param allowReconfigure Controls whether you are allowed to call this methods multiple times. It is **dangerours**, but possible if really needed. Default value is false
     * */
    @Throws(RuntimeException::class)
    fun configure(config: TIMConfiguration, customLogger: TIMLogger? = TIMLoggerInternal(), context: Context, timBiometricUtil: TIMBiometricData = TIMBiometricData.Builder().build(), allowReconfigure: Boolean = false) {

        if (!allowReconfigure && (_storage != null || _auth != null)) {
            throw RuntimeException("⛔️ You shouldn't configure TIM more than once!")
        }

        _logger = customLogger

        val applicationContext = context.applicationContext

        val encryptedStorage = TIMEncryptedStorage(
            TIMEncryptedSharedPreferences(applicationContext),
            TIMEncryptedStorageLoggerInternal(),
            TIMKeyServiceImpl.getInstance(config.keyServiceConfig),
            config.encryptionMethod,
        )

        val storage = TIMDataStorageInternal(
            encryptedStorage,
            timBiometricUtil
        )
        _auth = TIMAuthInternal(
            storage,
            AppAuthController(config.oidcConfig, applicationContext),
            TIMAppBackgroundMonitorInternal()
        )

        _storage = storage
    }

    /**
     * Configures [TIM] properties with the provided custom implementations
     * **THIS IS NOT THE USUAL WAY TO CONFIGURE TIM**
     * This can be useful when mocking for testing or other scenarios where custom implementation
     *
     * @param dataStorage implementation of [TIMDataStorage]
     * @param auth implementation of [TIMAuth]
     */
    fun configure(dataStorage: TIMDataStorage, auth: TIMAuth) {
        _storage = dataStorage
        _auth = auth
    }

    //region biometric related util functions

    /**
     * Checks if Biometric Authentication is ready for the device
     * @param context a context object
     * @return true if the device is ready for biometric authentication
     */
    fun isBiometricReady(context: Context) = BiometricUtil.isBiometricReady(context)

    /**
     * Get the Biometric capability directly as a AuthenticationStatus Int. For more fine-grained handling of device biometric capability
     * @param context a context object
     * @return A [BiometricManager] AuthenticationStatus Int
     */
    fun hasBiometricCapability(context: Context) = BiometricUtil.hasBiometricCapability(context)

    /**
     * Create a intent that can be used to display a [Settings.ACTION_SECURITY_SETTINGS], the device settings screen for biometric setup
     * @return a [Intent] the can be used with [startActivityForResult] to resume biometric login flow after changing biometric auth settings
     */
    fun createBiometricSettingsIntent() = BiometricUtil.launchBiometricSettings()

    /**
     * Get a intent that can be used to display a [Settings.ACTION_BIOMETRIC_ENROLL] action
     * @return a [Intent] the can be used with [startActivityForResult] to resume biometric login flow after the user has completed biometric enrollment
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createBiometricEnrollmentIntent() = BiometricUtil.configureBiometricEnrollmentIntent()

    //endregion
}