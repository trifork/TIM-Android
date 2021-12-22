package com.trifork.timandroid

import android.content.Context
import android.util.Log
import com.trifork.timandroid.appauth.AppAuthController
import com.trifork.timandroid.helpers.TIMEncryptedStorageLoggerInternal
import com.trifork.timandroid.helpers.TIMLogger
import com.trifork.timandroid.helpers.TIMLoggerInternal
import com.trifork.timandroid.internal.TIMAuthInternal
import com.trifork.timandroid.internal.TIMDataStorageInternal
import com.trifork.timandroid.models.TIMConfiguration
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.helpers.TIMEncryptedStorageLogger
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
    fun configure(config: TIMConfiguration, customLogger: TIMLogger? = TIMLoggerInternal(), context: Context, allowReconfigure: Boolean = false) {

        if (!allowReconfigure && (_storage != null || _auth != null)) {
            throw RuntimeException("⛔️ You shouldn't configure TIM more than once!")
        }

        _logger = customLogger

        val encryptedStorage = TIMEncryptedStorage(
            TIMEncryptedSharedPreferences(context),
            TIMEncryptedStorageLoggerInternal(),
            TIMKeyServiceImpl.getInstance(config.keyServiceConfig),
            config.encryptionMethod,
        )

        val storage = TIMDataStorageInternal(encryptedStorage)
        _auth = TIMAuthInternal(
            storage,
            AppAuthController(config.oidcConfig, context)
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
}