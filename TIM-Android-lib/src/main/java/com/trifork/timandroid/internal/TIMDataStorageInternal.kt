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
                    is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(jwtResult.error).toTIMFailure()
                    is TIMResult.Success -> jwtResult
                }
            }
            is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(refreshToken.error).toTIMFailure()
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

        TIM.logger?.log(DEBUG, TAG, "Got key from key server")

        return encryptedStorage.hasBiometricProtectedValue(TIMDataStorageKey.RefreshToken(userId).storageKey, keyId)
    }

    override fun disableBiometricAccessForRefreshToken(userId: String) {
        val keyIdResult = getAndTryConvertCallback(TIMDataStorageKey.KeyId(userId)) {
            it.convertToString()
        }

        if (keyIdResult is TIMResult.Success) encryptedStorage.removeLongSecret(keyIdResult.value)
    }

    override fun getStoredRefreshTokenViaBiometric(scope: CoroutineScope, userId: String, fragment: Fragment): Deferred<TIMResult<BiometricRefreshToken, TIMError>> = scope.async {
        val keyIdResult = getUserIdKeyId(userId)

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return@async keyIdResult
            is TIMResult.Success -> keyIdResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got key id")

        val decryptionCipherResult = getBiometricCipher(scope, fragment) {
            encryptedStorage.getDecryptCipher(keyId)
        }.await()

        val decryptionCipher = when (decryptionCipherResult) {
            is TIMResult.Failure -> return@async decryptionCipherResult
            is TIMResult.Success -> decryptionCipherResult.value
        }

        TIM.logger?.log(DEBUG, TAG, "Got decryption cipher for biometric prompt")

        val getViaBiometricResult = encryptedStorage.getViaBiometric(scope, TIMDataStorageKey.RefreshToken(userId).storageKey, keyId, decryptionCipher).await()

        return@async when (getViaBiometricResult) {
            is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(getViaBiometricResult.error).toTIMFailure()
            is TIMResult.Success -> {
                val jwtResult = JWT.newInstance(getViaBiometricResult.value.data.convertToString())

                when (jwtResult) {
                    is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(jwtResult.error).toTIMFailure()
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

    override fun enableBiometricAccessForRefreshToken(scope: CoroutineScope, password: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>> {
        return enableBiometricAccess(scope, userId, fragment) { keyId, encryptionCipher ->
            encryptedStorage.enableBiometric(scope, keyId, password, encryptionCipher)
        }
    }

    override fun enableBiometricAccessForRefreshTokenLongSecret(scope: CoroutineScope, longSecret: String, userId: String, fragment: Fragment): Deferred<TIMResult<Unit, TIMError>> {
        //Enable biometric access, wrapped in a scope to be compatible with helper function
        return enableBiometricAccess(scope, userId, fragment) { keyId, encryptionCipher ->
            scope.async {
                encryptedStorage.enableBiometric(keyId, longSecret, encryptionCipher)
            }
        }
    }

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
            is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(storeResult.error).toTIMFailure()
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
     * Enable biometric access to some resource for a given userId
     * @param scope the [CoroutineScope]
     * @param userId the userId for the user which should have biometric access to the resource
     * @param fragment for showing the biometric prompt
     * @param enableBiometricAccessFunction the function that determines the specific biometric access
     * @return a unit in case of success or a mapped TIMEncryptedStorageError
     */
    private fun enableBiometricAccess(scope: CoroutineScope, userId: String, fragment: Fragment, enableBiometricAccessFunction: (keyId: String, encryptionCipher: Cipher) -> Deferred<TIMResult<Unit, TIMEncryptedStorageError>>) = scope.async {
        //Get the user id key id
        val keyIdResult = getUserIdKeyId(userId)

        val keyId = when (keyIdResult) {
            is TIMResult.Failure -> return@async keyIdResult
            is TIMResult.Success -> keyIdResult.value
        }

        //Use getBiometricCipher to get the biometric cipher for encryption
        val encryptionCipherResult = getBiometricCipher(scope, fragment) {
            encryptedStorage.getEncryptCipher()
        }.await()

        val encryptionCipher = when (encryptionCipherResult) {
            is TIMResult.Failure -> return@async encryptionCipherResult
            is TIMResult.Success -> encryptionCipherResult.value
        }

        //Calls the provided biometric access function
        val enableBiometricResult = enableBiometricAccessFunction(keyId, encryptionCipher).await()

        return@async when (enableBiometricResult) {
            is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(enableBiometricResult.error).toTIMFailure()
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
    private fun getBiometricCipher(scope: CoroutineScope, fragment: Fragment, getInitialCipher: () -> TIMResult<Cipher, TIMEncryptedStorageError>): Deferred<TIMResult<Cipher, TIMError>> = scope.async {
        //Calls the provided getInitialCipher function, returns a cipher for either encryption or decryption
        val initialCipherResult = getInitialCipher()

        val initialCipher = when (initialCipherResult) {
            is TIMResult.Failure -> return@async TIMError.Storage(TIMStorageError.EncryptedStorageFailed(initialCipherResult.error)).toTIMFailure()
            is TIMResult.Success -> initialCipherResult.value
        }

        //Presents the biometric prompt, receiving our new cipher
        val cipherResult = TIMBiometric.presentBiometricPrompt(scope, timBiometricUtil, fragment, initialCipher).await()

        return@async when (cipherResult) {
            is TIMResult.Failure -> TIMError.Storage(cipherResult.error).toTIMFailure()
            is TIMResult.Success -> cipherResult.value.toTIMSuccess()
        }
    }

    //endregion

}