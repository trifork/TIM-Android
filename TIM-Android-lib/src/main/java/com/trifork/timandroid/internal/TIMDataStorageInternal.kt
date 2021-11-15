package com.trifork.timandroid.internal

import com.trifork.timandroid.TIMDataStorage
import com.trifork.timencryptedstorage.StorageKey
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import com.trifork.timencryptedstorage.models.toTIMSucces

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
    private val encryptedStorage: TIMEncryptedStorage
) : TIMDataStorage {

    //region Available user ids
    override val availableUserids: Set<String>
        get() {
            val result = get<Set<String>?>(TIMDataStorageKey.AvailableUserIds) {
                TODO("Figure out how to convert to a valid type for EncryptedStorage (Potentially Serialization)")
                null
            }
            return (result as? TIMResult.Success)?.value ?: emptySet()
        }

    private fun addAvailableUserId(userId: String) {
        val availableIds = availableUserids.toMutableSet()
        availableIds.add(userId)
        store(
            TODO("Figure out how to convert to a valid type for EncryptedStorage (Potentially Serialization)"),
            TIMDataStorageKey.AvailableUserIds
        )
    }

    private fun removeAvailableUserId(userId: String) {
        val availableIds = availableUserids.toMutableSet()
        availableIds.remove(userId)
        store(
            TODO("Figure out how to convert to a valid type for EncryptedStorage (Potentially Serialization)"),
            TIMDataStorageKey.AvailableUserIds
        )
    }
    //endregion

    //region NON-SECURE private read/write helpers
    private fun <T> get(
        key: TIMDataStorageKey,
        convertCallback: (ByteArray) -> T
    ): TIMResult<T, TIMSecureStorageError> {
        val dataResult = encryptedStorage.secureStorage.get(key.storageKey)
        return when (dataResult) {
            is TIMResult.Success -> convertCallback(dataResult.value).toTIMSucces()
            is TIMResult.Failure -> dataResult
        }
    }

    private fun store(data: ByteArray, key: TIMDataStorageKey) {
        encryptedStorage.secureStorage.store(data, key.storageKey)
    }
    //endregion

    override fun clear(userId: String) {
        disableCurrentBiometricAccess(userId)

        TIMDataStorageKey.getUserSpecificCases(userId).forEach {
            encryptedStorage.remove(it.storageKey)
        }

        removeAvailableUserId(userId)
    }

}