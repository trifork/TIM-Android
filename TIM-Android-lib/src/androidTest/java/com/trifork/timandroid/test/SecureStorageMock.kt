package com.trifork.timencryptedstorage.test

import com.trifork.timencryptedstorage.StorageKey
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage

class SecureStorageMock : TIMSecureStorage {
    //private val bioProtectedData = HashMap<SecureStorageMockItem, ByteArray>()
    private val protectedData = HashMap<String, ByteArray>()

    override fun remove(storageKey: StorageKey) {
        protectedData.remove(storageKey)
    }

    override fun store(data: ByteArray, storageKey: StorageKey) {
        protectedData[storageKey] = data
        return
    }

    override fun get(storageKey: StorageKey): TIMResult<ByteArray, TIMSecureStorageError> {
        val data = protectedData[storageKey]
        if (data != null) {
            return data.toTIMSuccess()
        }
        return TIMSecureStorageError.FailedToLoadData(Throwable("No data found for key $storageKey")).toTIMFailure()
    }

    override fun hasValue(storageKey: StorageKey): Boolean {
        return protectedData.containsKey(storageKey)
    }

    override fun storeBiometricProtected(data: ByteArray, storageKey: StorageKey): TIMResult<Unit, TIMSecureStorageError> {
        protectedData[storageKey] = data
        return Unit.toTIMSuccess()
    }

    override fun hasBiometricProtectedValue(storageKey: StorageKey): Boolean {
        return protectedData.containsKey(storageKey)
    }

    override fun getBiometricProtected(storageKey: StorageKey): TIMResult<ByteArray, TIMSecureStorageError> {
        return get(storageKey)
    }

    fun clear() {
        protectedData.clear()
    }
}