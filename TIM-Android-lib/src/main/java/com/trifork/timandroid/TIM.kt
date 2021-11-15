package com.trifork.timandroid

import android.content.Context
import com.trifork.timandroid.appauth.AppAuthController
import com.trifork.timandroid.internal.TIMAuthInternal
import com.trifork.timandroid.internal.TIMDataStorageInternal
import com.trifork.timandroid.models.TIMConfiguration
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.keyservice.TIMKeyServiceImpl
import com.trifork.timencryptedstorage.securestorage.TIMEncryptedSharedPreferences

// TODO: In the README.md it should contain a section about the wierd quirk between redirectUri in BuildConfig and in Manifest ("app:/" vs "app") - MFJ (20/09/2021)

object TIM {

    private var _storage: TIMDataStorage? = null
    val storage: TIMDataStorage
        @Throws(RuntimeException::class)
        get() =
            _storage
                ?: throw RuntimeException("Accessing TIM.storage before calling TIM.configure(...) is not allowed!")

    private var _auth: TIMAuth? = null
    val auth: TIMAuth
        @Throws(RuntimeException::class)
        get() = _auth
            ?: throw RuntimeException("Accessing TIM.auth before calling TIM.configure(...) is not allowed!")

    /**
     * Indicates whether [TIM] has been configure by a call to [configure]
     */
    val isConfigured
        get() = _auth != null && _storage != null

    // TODO: Consider weak reference here - MFJ (14/09/2021)
    /**
     * // TODO: Missing docs - MFJ (20/09/2021)
     */
    @Throws(RuntimeException::class)
    fun configure(config: TIMConfiguration, context: Context) {

        if(_storage != null || _auth != null) {
            throw RuntimeException("⛔️ You shouldn't configure TIM more than once!")
        }

        val encryptedStorage = TIMEncryptedStorage(
            TIMEncryptedSharedPreferences(context),
            TIMKeyServiceImpl.getInstance(config.keyServiceConfig),
            config.encryptionMethod
        )

        val storage = TIMDataStorageInternal(encryptedStorage)
        val auth = TIMAuthInternal(
            storage,
            AppAuthController(config.oidcConfig,)
        )
    }

    /**
     * Configures [TIM] properties with the provided custom implementations
     *
     * This can be useful when mocking for testing or other scenarios where custom implementation
     */
    fun configure(dataStorage: TIMDataStorage, auth: TIMAuth) {
        _storage = dataStorage
        _auth = auth
    }
}


