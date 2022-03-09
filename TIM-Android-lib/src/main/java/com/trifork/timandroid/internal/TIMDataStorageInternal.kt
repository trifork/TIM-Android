package com.trifork.timandroid.internal

import android.util.Log.DEBUG
import androidx.fragment.app.Fragment
import com.trifork.timandroid.TIM
import com.trifork.timandroid.TIMDataStorage
import com.trifork.timandroid.biometric.TIMBiometric
import com.trifork.timandroid.biometric.TIMBiometricData
import com.trifork.timandroid.helpers.BiometricRefreshToken
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.helpers.ext.convertToByteArray
import com.trifork.timandroid.helpers.ext.convertToString
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timandroid.models.errors.TIMStorageError
import com.trifork.timencryptedstorage.StorageKey
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.shared.extensions.asPreservedByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.crypto.Cipher

internal sealed class TIMDataStorageKey {
    class KeyId(val userId: String) : TIMDataStorageKey()
    class RefreshToken(val userId: String) : TIMDataStorageKey()
    object AvailableUserIds : TIMDataStorageKey()

    val storageKey: StorageKey
        get() = when (this) {
            AvailableUserIds -> "availableUserIds"
            is KeyId -> "keyId_$userId"
            is RefreshToken -> "refreshToken_$userId"
        }

    companion object {
        fun getUserSpecificCases(userId: String) = listOf(
            KeyId(userId),
            RefreshToken(userId)
        )
    }
}

internal class TIMDataStorageInternal(
    private val encryptedStorage: TIMEncryptedStorage,
    private val timBiometricUtil: TIMBiometricData
) : TIMDataStorage {

    companion object {
        val TAG = "TIMDataStorageInternal"
    }

    //region Available user ids
    override val availableUserIds: Set<String>
        get() {
            val result = getAndTryConvertCallback<Set<String>?>(TIMDataStorageKey.AvailableUserIds) {
                Json.decodeFromString(it.convertToString())
            }
            return (result as? TIMResult.Success)?.value ?: emptySet()
        }

    private fun addAvailableUserId(userId: String) {
        val availableIds = availableUserIds.toMutableSet()
        availableIds.add(userId)

        store(
            Json.encodeToString(availableIds).convertToByteArray(),
            TIMDataStorageKey.AvailableUserIds
        )
    }

    private fun removeAvailableUserId(userId: String) {
        val availableIds = availableUserIds.toMutableSet()
        availableIds.remove(userId)
        store(
            Json.encodeToString(availableIds).convertToByteArray(),
            TIMDataStorageKey.AvailableUserIds
        )
    }
    //endregion

    //region Password overrides
    override fun hasRefreshToken(userId: String): Boolean =
        encryptedStorage.hasValue(TIMDataStorageKey.RefreshToken(userId).storageKey) &&
                encryptedStorage.hasValue(TIMDataStorageKey.KeyId(userId).storageKey)

    override fun getStoredRefreshToken(scope: CoroutineScope, userId: String, password: String): Deferred<TIMResult<JWT, TIMError>> = scope.async {
        val keyIdResult = getUserIdKeyId(userId)

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return@async keyIdResult
            is TIMResult.Success -> keyIdResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got key")

        val storageKey = TIMDataStorageKey.RefreshToken(userId).storageKey

        val refreshToken = encryptedStorage.get(scope, storageKey, keyId, password).await()

        return@async when (refreshToken) {
            is TIMResult.Success -> {
                TIM.logger?.log(DEBUG, TAG, "Got refresh token")
                val jwtResult = JWT.newInstance(refreshToken.value.convertToString())

                when (jwtResult) {
                    is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(jwtResult.error)).toTIMFailure()
                    is TIMResult.Success -> jwtResult
                }
            }
            is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(refreshToken.error)).toTIMFailure()
        }
    }

    override fun storeRefreshTokenWithExistingPassword(scope: CoroutineScope, refreshToken: JWT, password: String) = scope.async {
        val keyIdResult = getAndTryConvertCallback(TIMDataStorageKey.KeyId(refreshToken.userId)) {
            it.convertToString()
        }

        val keyId = when (keyIdResult) {
            is TIMResult.Success -> keyIdResult.value
            is TIMResult.Failure -> return@async mapAndHandleKeyIdLoadError(keyIdResult.error, refreshToken.userId).toTIMFailure()
        }

        TIM.logger?.log(DEBUG, TAG, "Got key id")

        val storeResult = encryptedStorage.store(scope, TIMDataStorageKey.RefreshToken(refreshToken.userId).storageKey, refreshToken.token.convertToByteArray(), keyId, password).await()

        return@async when (storeResult) {
            is TIMResult.Success -> {
                addAvailableUserId(refreshToken.userId)
                TIM.logger?.log(DEBUG, TAG, "Stored refreshToken and added userId to available user ids")
                Unit.toTIMSuccess()
            }
            is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(storeResult.error)).toTIMFailure()
        }
    }

    override fun storeRefreshTokenWithNewPassword(scope: CoroutineScope, refreshToken: JWT, password: String) = scope.async {
        val storeWithNewKeyResult = encryptedStorage.storeWithNewKey(scope, TIMDataStorageKey.RefreshToken(refreshToken.userId).storageKey, refreshToken.token.convertToByteArray(), password).await()

        val keyCreation = when (storeWithNewKeyResult) {
            is TIMResult.Failure -> return@async TIMError.Storage(TIMStorageError.EncryptedStorageFailed(storeWithNewKeyResult.error)).toTIMFailure()
            is TIMResult.Success -> storeWithNewKeyResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Created new key using key server")

        disableBiometricAccessForRefreshToken(refreshToken.userId)
        store(keyCreation.keyId.convertToByteArray(), TIMDataStorageKey.KeyId(refreshToken.userId))

        addAvailableUserId(refreshToken.userId)

        TIM.logger?.log(DEBUG, TAG, "Stored new key and added user id to available user ids")

        return@async keyCreation.toTIMSuccess()
    }
    //endregion

    //region Biometric overrides
    override fun hasBiometricAccessForRefreshToken(userId: String): Boolean {
        val keyIdResult = getAndTryConvertCallback(TIMDataStorageKey.KeyId(userId)) {
            it.convertToString()
        }

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return false
            is TIMResult.Success -> keyIdResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got key from local storage")

        return encryptedStorage.hasBiometricProtectedValue(TIMDataStorageKey.RefreshToken(userId).storageKey, keyId)
    }

    /**
     * Removes the long secret from the encrypted storage for the parsed userId
     * @param userId the userId for which the biometric access should be removed
     */
    override fun disableBiometricAccessForRefreshToken(userId: String) {
        val keyIdResult = getAndTryConvertCallback(TIMDataStorageKey.KeyId(userId)) {
            it.convertToString()
        }

        if (keyIdResult is TIMResult.Success) encryptedStorage.removeLongSecret(keyIdResult.value)
    }

    /**
     * Gets the stored refresh token using the encrypted storage and biometric prompt
     * @param scope the scope for the task
     * @param userId the user userId
     * @param fragment for showing the biometric prompt
     * @return a [BiometricRefreshToken] in case of sucess or [TIMError] in case of an error. In case of [TIMEncryptedStorageError.PermanentlyInvalidatedKey] the [disableBiometricAccessForRefreshToken] function is called disabling biometric access
     */
    override fun getStoredRefreshTokenViaBiometric(scope: CoroutineScope, userId: String, fragment: Fragment): Deferred<TIMResult<BiometricRefreshToken, TIMError>> = scope.async {
        val keyIdResult = getUserIdKeyId(userId)

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return@async keyIdResult
            is TIMResult.Success -> keyIdResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got key id")

        val initialCipherResult = encryptedStorage.getDecryptCipher(keyId)

        TIM.logger?.log(DEBUG, TAG, "Got initial cipher result: $initialCipherResult")

        val initialCipher = when (initialCipherResult) {
            is TIMResult.Failure -> return@async mapAndHandleGetInitialBiometricDecryptionCipherError(initialCipherResult.error, userId).toTIMFailure()
            is TIMResult.Success -> initialCipherResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got initial cipher successfully")

        val decryptionCipherResult = getBiometricCipher(scope, fragment, initialCipher).await()

        TIM.logger?.log(DEBUG, TAG, "Got decryption cipher: $decryptionCipherResult")

        val decryptionCipher = when (decryptionCipherResult) {
            is TIMResult.Failure -> return@async decryptionCipherResult.error.toTIMFailure()
            is TIMResult.Success -> decryptionCipherResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got decryption cipher successfully")

        val getViaBiometricResult = encryptedStorage.getViaBiometric(scope, TIMDataStorageKey.RefreshToken(userId).storageKey, keyId, decryptionCipher).await()

        return@async when (getViaBiometricResult) {
            is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(getViaBiometricResult.error)).toTIMFailure()
            is TIMResult.Success -> {
                val jwtResult = JWT.newInstance(getViaBiometricResult.value.data.convertToString())

                when (jwtResult) {
                    is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(jwtResult.error)).toTIMFailure()
                    is TIMResult.Success -> {
                        TIM.logger?.log(DEBUG, TAG, "Decoded JWT successfully, returning BiometricRefreshToken")
                        BiometricRefreshToken(
                            jwtResult.value,
                            getViaBiometricResult.value.longSecret
                        ).toTIMSuccess()
                    }
                }
            }
        }
    }

    /**
     * Used to enable biometric access. Can be used to enable biometric access after the user has logged in and the password has been requested. Verifies the password against the keyserver and requests a longSecret for biometric encryption.
     * @param scope the scope for the task
     * @param password the password for the provided userId
     * @param userId the users user id
     * @param fragment the fragment used for showing the biometric prompt
     * @return returns [Unit] in case of success and [TIMError] wrapping more specific TIM errors
     */
    override fun enableBiometricAccessForRefreshToken(scope: CoroutineScope, password: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>> {
        return getKeyIdAndBiometricCipherEnableBiometricAccess(scope, userId, fragment) { keyId, encryptionCipher ->
            encryptedStorage.enableBiometric(scope, keyId, password, encryptionCipher)
        }
    }

    /**
     * Used to enable biometric access. Used during signup/registration flow where we have the longSecret available.
     * @param scope the scope for the task
     * @param longSecret the longSecret gained from the signup/registration flow
     * @param userId the users user id
     * @param fragment the fragment used for showing the biometric prompt
     * @return returns [Unit] in case of success and [TIMError] wrapping more specific TIM errors
     */
    override fun enableBiometricAccessForRefreshTokenLongSecret(scope: CoroutineScope, longSecret: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>> {
        //Enable biometric access, wrapped in a scope to be compatible with helper function
        return getKeyIdAndBiometricCipherEnableBiometricAccess(scope, userId, fragment) { keyId, encryptionCipher ->
            scope.async {
                encryptedStorage.enableBiometric(keyId, longSecret, encryptionCipher)
            }
        }
    }

    /**
     * Used to update the refresh token after biometric login
     * @param scope the scope for the task
     * @param refreshToken the refreshToken gained from biometric login
     * @param longSecret The long secret obtained from biometric login
     * @return returns [Unit] in case of success and [TIMError] wrapping more specific TIM errors
     */
    override fun storeRefreshTokenWithLongSecret(scope: CoroutineScope, refreshToken: JWT, longSecret: String): Deferred<TIMResult<Unit, TIMError>> = scope.async {
        val keyIdResult = getAndTryConvertCallback(TIMDataStorageKey.KeyId(refreshToken.userId)) {
            it.convertToString()
        }

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return@async mapAndHandleKeyIdLoadError(keyIdResult.error, refreshToken.userId).toTIMFailure()
            is TIMResult.Success -> keyIdResult.value
        }

        val storeResult = encryptedStorage.storeWithLongSecret(
            scope,
            TIMDataStorageKey.RefreshToken(refreshToken.userId).storageKey,
            refreshToken.token.asPreservedByteArray,
            keyId,
            longSecret
        ).await()

        return@async when (storeResult) {
            is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(storeResult.error)).toTIMFailure()
            is TIMResult.Success -> Unit.toTIMSuccess()
        }
    }

    //endregion
    override fun clear(userId: String) {
        TIMDataStorageKey.getUserSpecificCases(userId).forEach {
            encryptedStorage.remove(it.storageKey)
        }

        removeAvailableUserId(userId)
    }

    //region NON-SECURE private read/write helpers
    private fun <T> getAndTryConvertCallback(
        key: TIMDataStorageKey,
        convertCallback: (ByteArray) -> T
    ): TIMResult<T, TIMSecureStorageError> {
        val dataResult = encryptedStorage.secureStorage.get(key.storageKey)
        val data = when (dataResult) {
            is TIMResult.Success -> dataResult.value
            is TIMResult.Failure -> return dataResult
        }
        //Try to convert using provided convert function
        return try {
            convertCallback(data).toTIMSuccess()
        } catch (throwable: Throwable) {
            TIMSecureStorageError.UnrecoverablyFailedToConvertData(throwable).toTIMFailure()
        }
    }

    private fun store(data: ByteArray, key: TIMDataStorageKey) {
        encryptedStorage.secureStorage.store(data, key.storageKey)
    }
    //endregion

    //region helpers
    private fun getUserIdKeyId(userId: String): TIMResult<String, TIMError> {
        val keyIdResult = getAndTryConvertCallback(TIMDataStorageKey.KeyId(userId)) {
            it.convertToString()
        }

        return when (keyIdResult) {
            is TIMResult.Failure -> mapAndHandleKeyIdLoadError(keyIdResult.error, userId).toTIMFailure()
            is TIMResult.Success -> keyIdResult.value.toTIMSuccess()
        }
    }

    private fun mapAndHandleKeyIdLoadError(secureStorageError: TIMSecureStorageError, userId: String): TIMError {
        return when (secureStorageError) {
            is TIMSecureStorageError.FailedToStoreData -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.SecureStorageFailed(secureStorageError)))
            else -> {
                clear(userId)
                TIMError.Storage(TIMStorageError.IncompleteUserDataSet())
            }
        }
    }
    //endregion

    //region Biometric helpers
    /**
     * Enable biometric access to some resource for a given userId.
     * In order for us to enable biometric access, we need to fetch the key id, get a cipher from the biometric prompt and then save the resource.
     * The functions goes through 3 steps: 1. get keyId. 2. Get cipher from user biometrics. 3. Run and handle result from parsed [enableBiometricAccessFunction].
     * @param scope the [CoroutineScope]
     * @param userId the userId for the user which should have biometric access to the resource
     * @param fragment for showing the biometric prompt
     * @param enableBiometricAccessFunction the function that determines the specific biometric access
     * @return a unit in case of success or a mapped TIMEncryptedStorageError
     */
    private fun getKeyIdAndBiometricCipherEnableBiometricAccess(scope: CoroutineScope, userId: String, fragment: Fragment, enableBiometricAccessFunction: (keyId: String, encryptionCipher: Cipher) -> Deferred<TIMResult<Unit, TIMEncryptedStorageError>>) = scope.async {
        //Get the user id key id
        val keyIdResult = getUserIdKeyId(userId)

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return@async keyIdResult
            is TIMResult.Success -> keyIdResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "getKeyIdAndBiometricCipherEnableBiometricAccess: Got key id")

        val initialCipherResult = encryptedStorage.getEncryptCipher(keyId)

        //If the decrypt cipher fails, wrap the error in a TIMError.Storage and TIMStorageError.EncryptedStorageFailed error
        val initialCipher = when (initialCipherResult) {
            is TIMResult.Failure -> return@async TIMError.Storage(TIMStorageError.EncryptedStorageFailed(initialCipherResult.error)).toTIMFailure()
            is TIMResult.Success -> initialCipherResult.value
        }

        val encryptionCipherResult = getBiometricCipher(scope, fragment, initialCipher).await()

        TIM.logger?.log(DEBUG, TAG, "getKeyIdAndBiometricCipherEnableBiometricAccess: encryptionCipherResult: $encryptionCipherResult")

        val encryptionCipher = when (encryptionCipherResult) {
            is TIMResult.Failure -> return@async encryptionCipherResult
            is TIMResult.Success -> encryptionCipherResult.value
        }

        //Calls the provided biometric access function
        val enableBiometricResult = enableBiometricAccessFunction(keyId, encryptionCipher).await()

        return@async when (enableBiometricResult) {
            is TIMResult.Failure -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(enableBiometricResult.error)).toTIMFailure()
            is TIMResult.Success -> enableBiometricResult.value.toTIMSuccess()
        }
    }

    /**
     * Get a encryption or decryption cipher from the biometric prompt. Calls the provided getInitialCipher function to get the initial cipher sent to the prompt.
     * @param scope the [CoroutineScope]
     * @param fragment for showing the biometric prompt
     * @param getInitialCipher the function that returns the required cipher, either for encryption or decryption
     * @return the cipher from the biometric prompt or a mapped TIMError
     */
    private fun getBiometricCipher(scope: CoroutineScope, fragment: Fragment, initialCipher: Cipher): Deferred<TIMResult<Cipher, TIMError>> = scope.async {
        //Presents the biometric prompt, receiving our new cipher
        val cipherResult = TIMBiometric.presentBiometricPrompt(scope, timBiometricUtil, fragment, initialCipher).await()

        return@async when (cipherResult) {
            is TIMResult.Failure -> TIMError.Storage(cipherResult.error).toTIMFailure()
            is TIMResult.Success -> cipherResult.value.toTIMSuccess()
        }
    }

    /**
     * Disables biometric access for refresh token for the parsed userId in case the storage error is a PermanentlyInvalidatedKey exception, which is thrown in case the decryption key was bad and the user needs to re-authenticate and re-enable biometric authentication.
     * @param storageError the [TIMEncryptedStorageError] received from the [BiometricCipherHelper.getDecryptCipher] function
     * @param userId the userId for the user trying to authenticate
     * @return a [TIMError.Storage] error wrapping the thrown error
     */
    private fun mapAndHandleGetInitialBiometricDecryptionCipherError(storageError: TIMEncryptedStorageError, userId: String): TIMError {
        when (storageError) {
            is TIMEncryptedStorageError.PermanentlyInvalidatedKey -> {
                disableBiometricAccessForRefreshToken(userId)
            }
            else -> {}
        }

        return TIMError.Storage(TIMStorageError.EncryptedStorageFailed(storageError))
    }

    //endregion

}