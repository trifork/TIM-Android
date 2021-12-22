package com.trifork.timandroid.internal

import android.util.Log
import androidx.fragment.app.Fragment
import com.trifork.timandroid.TIMDataStorage
import com.trifork.timandroid.biometric.TIMBiometric
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
) : TIMDataStorage {

    //region Available user ids
    override val availableUserIds: Set<String>
        get() {
            val result = get<Set<String>?>(TIMDataStorageKey.AvailableUserIds) {
                //TODO Can actually fail if the stored string is in a different format, handle JsonDecodingException
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


    override fun hasRefreshToken(userId: String): Boolean =
        encryptedStorage.hasValue(TIMDataStorageKey.RefreshToken(userId).storageKey) &&
                encryptedStorage.hasValue(TIMDataStorageKey.KeyId(userId).storageKey)

    override fun hasBiometricAccessForRefreshToken(userId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun disableBiometricAccessForRefreshToken(userId: String) {
        val keyIdResult = get(TIMDataStorageKey.KeyId(userId)) {
            it.convertToString()
        }

        if (keyIdResult is TIMResult.Success) encryptedStorage.removeLongSecret(keyIdResult.value)
    }

    override fun getStoredRefreshToken(scope: CoroutineScope, userId: String, password: String): Deferred<TIMResult<JWT, TIMError>> = scope.async {
        val resultKeyId = get(TIMDataStorageKey.KeyId(userId)) {
            it.convertToString()
        }

        val keyId = when (resultKeyId) {
            is TIMResult.Success -> resultKeyId.value
            is TIMResult.Failure -> return@async mapAndHandleKeyIdLoadError(resultKeyId.error, userId).toTIMFailure()
        }

        val storageKey = TIMDataStorageKey.RefreshToken(userId).storageKey

        val refreshToken = encryptedStorage.get(scope, storageKey, keyId, password).await()

        return@async when (refreshToken) {
            is TIMResult.Success -> {
                val jwt = JWT.newInstance(refreshToken.value.convertToString())
                if (jwt != null) {
                    jwt.toTIMSuccess()
                }
                else {
                    TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.UnexpectedData()).toTIMFailure()
                }
            }
            is TIMResult.Failure -> TIMStorageError.EncryptedStorageFailed(refreshToken.error).toTIMFailure()
        }
    }

    private fun mapAndHandleKeyIdLoadError(secureStorageError: TIMSecureStorageError, userId: String): TIMError {
        return when (secureStorageError) {
            is TIMSecureStorageError.FailedToLoadData -> {
                clear(userId)
                TIMError.Storage(TIMStorageError.IncompleteUserDataSet())
            }
            is TIMSecureStorageError.FailedToStoreData -> TIMError.Storage(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.SecureStorageFailed(secureStorageError)))
            is TIMSecureStorageError.AuthenticationFailedForData -> TODO("Not yet implemented")
        }
    }

    override fun getStoredRefreshTokenViaBiometric(userId: String) : TIMResult<BiometricRefreshToken, TIMError> {
        TODO("Not yet implemented")
    }

    override fun storeRefreshTokenWithExistingPassword(scope: CoroutineScope, refreshToken: JWT, password: String) = scope.async {
        val keyIdResult = get(TIMDataStorageKey.KeyId(refreshToken.userId)) {
            it.convertToString()
        }

        when (keyIdResult) {
            is TIMResult.Success -> {
                val storeResult = encryptedStorage.store(scope, TIMDataStorageKey.RefreshToken(refreshToken.userId).storageKey, refreshToken.token.convertToByteArray(), keyIdResult.value, password).await()
                when (storeResult) {
                    is TIMResult.Success -> {
                        addAvailableUserId(refreshToken.userId)
                        return@async Unit.toTIMSuccess()
                    }
                    is TIMResult.Failure -> return@async TIMError.Storage(TIMStorageError.EncryptedStorageFailed(storeResult.error)).toTIMFailure()
                }
            }
            is TIMResult.Failure -> return@async mapAndHandleKeyIdLoadError(keyIdResult.error, refreshToken.userId).toTIMFailure()
        }
    }

    override fun storeRefreshTokenWithNewPassword(scope: CoroutineScope, refreshToken: JWT, password: String) = scope.async {
        val storeWithNewKeyResult = encryptedStorage.storeWithNewKey(scope, TIMDataStorageKey.RefreshToken(refreshToken.userId).storageKey, refreshToken.token.convertToByteArray(), password).await()

        //TODO Add error handling when storeWithNewKey throws errors correctly?
        disableBiometricAccessForRefreshToken(refreshToken.userId)
        store(storeWithNewKeyResult.value.keyId.convertToByteArray(), TIMDataStorageKey.KeyId(refreshToken.userId))

        addAvailableUserId(refreshToken.userId)

        return@async storeWithNewKeyResult.value.toTIMSuccess()
    }

    override fun enableBiometricAccessForRefreshToken(scope: CoroutineScope, password: String, userId: String, fragment: Fragment) : Deferred<TIMResult<Unit, TIMError>> = scope.async {

        val cipherResult = encryptedStorage.getEncryptCipher()

        val cipher = when (cipherResult) {
            is TIMResult.Failure -> TODO("No error handling")
            is TIMResult.Success -> cipherResult.value
        }

        val encryptionCipher = TIMBiometric.presentBiometricPrompt(scope, fragment, cipher).await()

        //TODO Parse received Cipher to encrypted storage?
        Log.d("here", "")
        return@async Unit.toTIMSuccess()
    }

    override fun storeRefreshTokenWithLongSecret(refreshToken: JWT, longSecret: String): TIMResult<Unit, TIMError> {
        TODO("Not yet implemented")
    }

    //region NON-SECURE private read/write helpers
    private fun <T> get(
        key: TIMDataStorageKey,
        convertCallback: (ByteArray) -> T
    ): TIMResult<T, TIMSecureStorageError> {
        val dataResult = encryptedStorage.secureStorage.get(key.storageKey)
        return when (dataResult) {
            is TIMResult.Success -> convertCallback(dataResult.value).toTIMSuccess()
            is TIMResult.Failure -> dataResult
        }
    }

    private fun store(data: ByteArray, key: TIMDataStorageKey) {
        encryptedStorage.secureStorage.store(data, key.storageKey)
    }
    //endregion

    override fun clear(userId: String) {
        //TODO(disableCurrentBiometricAccess(userId))

        TIMDataStorageKey.getUserSpecificCases(userId).forEach {
            encryptedStorage.remove(it.storageKey)
        }

        removeAvailableUserId(userId)
    }

}