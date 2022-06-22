package com.trifork.timandroid.models.errors

import com.trifork.timandroid.testHelpers.*
import com.trifork.timencryptedstorage.models.errors.*
import org.junit.*

class TIMErrorTests {
}

class TIMStorageErrorTests {
    companion object {
        val encryptedStorageFailed = TIMStorageError.EncryptedStorageFailed(
            TIMEncryptedStorageError.SecureStorageFailed(
                TIMSecureStorageError.FailedToLoadData(
                    Throwable("Test error!")
                )
            )
        )
        val badInternet = TIMStorageError.EncryptedStorageFailed(
            TIMEncryptedStorageError.KeyServiceFailed(
                TIMKeyServiceError.BadInternet()
            )
        )

        val badPassword = TIMStorageError.EncryptedStorageFailed(
            TIMEncryptedStorageError.KeyServiceFailed(
                TIMKeyServiceError.BadPassword()
            )
        )

        val keyLocked = TIMStorageError.EncryptedStorageFailed(
            TIMEncryptedStorageError.KeyServiceFailed(
                TIMKeyServiceError.KeyLocked()
            )
        )

    }

    @Test
    fun isKeyLocked() {
        encryptedStorageFailed.isKeyLocked().assertFalse()
        badInternet.isKeyLocked().assertFalse()
        badPassword.isKeyLocked().assertFalse()
        keyLocked.isKeyLocked().assertTrue()
    }

    @Test
    fun isWrongPassword() {
        encryptedStorageFailed.isWrongPassword().assertFalse()
        badInternet.isWrongPassword().assertFalse()
        keyLocked.isWrongPassword().assertFalse()
        badPassword.isWrongPassword().assertTrue()
    }

    @Test
    fun isKeyServiceError() {
        encryptedStorageFailed.isKeyServiceError().assertFalse()
        badInternet.isKeyServiceError().assertTrue()
        keyLocked.isKeyServiceError().assertTrue()
        badPassword.isKeyServiceError().assertTrue()
    }

    @Test
    fun isBiometricFailedError() {
        encryptedStorageFailed.isBiometricFailedError().assertTrue()
        badInternet.isBiometricFailedError().assertFalse()
        keyLocked.isBiometricFailedError().assertFalse()
        badPassword.isBiometricFailedError().assertFalse()
    }

}