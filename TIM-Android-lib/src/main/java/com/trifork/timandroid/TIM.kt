package com.trifork.timandroid

import android.app.*
import android.content.*
import android.os.*
import android.provider.*
import androidx.annotation.*
import androidx.biometric.*
import com.trifork.timandroid.appauth.*
import com.trifork.timandroid.biometric.*
import com.trifork.timandroid.helpers.*
import com.trifork.timandroid.internal.*
import com.trifork.timandroid.models.*
import com.trifork.timencryptedstorage.*
import com.trifork.timencryptedstorage.keyservice.*
import com.trifork.timencryptedstorage.securestorage.*

public open class TIMImpl {

    private var _storage: TIMDataStorage? = null
    public val storage: TIMDataStorage
        @Throws(RuntimeException::class)
        get() = _storage
            ?: throw RuntimeException("Accessing TIM.storage before calling TIM.configure(...) is not allowed!")

    private var _auth: TIMAuth? = null
    public val auth: TIMAuth
        @Throws(RuntimeException::class)
        get() = _auth
            ?: throw RuntimeException("Accessing TIM.auth before calling TIM.configure(...) is not allowed!")

    private var _logger: TIMLogger? = null
    public val logger: TIMLogger?
        get() = _logger

    /**
     * Indicates whether [TIM] has been configure by a call to [configure]
     */
    public val isConfigured: Boolean
        get() = _auth != null && _storage != null

    // TODO: Consider weak reference here - MFJ (14/09/2021)
    /**
     * @param config TIMConfiguration
     * @param context Context
     * @param allowReconfigure Controls whether you are allowed to call this methods multiple times. It is **dangerours**, but possible if really needed. Default value is false
     * */
    @Throws(RuntimeException::class)
    public fun configure(
        config: TIMConfiguration,
        customLogger: TIMLogger? = TIMLoggerInternal(),
        context: Context,
        timBiometricData: TIMBiometricData = TIMBiometricData.Builder().build(),
        allowReconfigure: Boolean = false
    ) {

        if (!allowReconfigure && isConfigured) {
            throw RuntimeException("⛔️ You shouldn't configure TIM more than once!")
        }

        val applicationContext = context.applicationContext

        val encryptedStorage = TIMEncryptedStorage(
            secureStorage = TIMEncryptedSharedPreferences(applicationContext),
            logger = TIMEncryptedStorageLoggerInternal(),
            keyService = TIMKeyServiceImpl.getInstance(config.keyServiceConfig),
            encryptionMethod = config.encryptionMethod,
        )

        val storage = TIMDataStorageInternal(
            encryptedStorage = encryptedStorage,
            timBiometricUtil = timBiometricData
        )


        val auth = TIMAuthInternal(
            storage = storage,
            openIdController = AppAuthController(config.oidcConfig, applicationContext),
            backgroundMonitor = TIMAppBackgroundMonitorInternal()
        )

        configure(
            dataStorage = storage,
            auth = auth,
            customLogger = customLogger
        )
    }

    /**
     * Configures [TIM] properties with the provided custom implementations
     * **THIS IS NOT THE USUAL WAY TO CONFIGURE TIM**
     * This can be useful when mocking for testing or other scenarios where custom implementation is necessary
     * @param dataStorage implementation of [TIMDataStorage]
     * @param auth implementation of [TIMAuth]
     */
    public fun configure(
        dataStorage: TIMDataStorage,
        auth: TIMAuth,
        customLogger: TIMLogger? = TIMLoggerInternal()
    ) {
        _storage = dataStorage
        _auth = auth
        _logger = customLogger
    }

    //region biometric related util functions

    /**
     * Checks if Biometric Authentication is ready for the device
     * @param context a context object
     * @return true if the device is ready for biometric authentication
     */
    public fun isBiometricReady(context: Context): Boolean = BiometricUtil.isBiometricReady(context)

    /**
     * Get the Biometric capability directly as a AuthenticationStatus Int. For more fine-grained handling of device biometric capability
     * @param context a context object
     * @return A [BiometricManager] AuthenticationStatus Int
     */
    public fun hasBiometricCapability(context: Context): TIMBiometricAuthentication = BiometricUtil.hasBiometricCapability(context)

    /**
     * Create a intent that can be used to display a [Settings.ACTION_SECURITY_SETTINGS], the device settings screen for biometric setup
     * @return a [Intent] the can be used with [Activity.startActivityForResult] to resume biometric login flow after changing biometric auth settings
     */
    public fun createBiometricSettingsIntent(): Intent = BiometricUtil.launchBiometricSettings()

    /**
     * Get a intent that can be used to display a [Settings.ACTION_BIOMETRIC_ENROLL] action
     * @return a [Intent] the can be used with [Activity.startActivityForResult] to resume biometric login flow after the user has completed biometric enrollment
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public fun createBiometricEnrollmentIntent(): Intent = BiometricUtil.configureBiometricEnrollmentIntent()

    //endregion
}

// TODO: In the README.md it should contain a section about the weird quirk between redirectUri in BuildConfig and in Manifest ("app:/" vs "app") - MFJ (20/09/2021)
public object TIM : TIMImpl()