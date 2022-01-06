package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.models.errors.TIMStorageError
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.errors.TIMSecureStorageError
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TIMStorageErrorTests {

    @Test
    fun testIsKeyLocked() {
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.SecureStorageFailed(TIMSecureStorageError.FailedToLoadData(Throwable("Test error!")))).isKeyLocked())
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadInternet())).isKeyLocked())
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadPassword())).isKeyLocked())
        Assert.assertTrue(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.KeyLocked())).isKeyLocked())
    }

    @Test
    fun testIsWrongPassword() {
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.SecureStorageFailed(TIMSecureStorageError.FailedToStoreData(Throwable("Test error!")))).isWrongPassword())
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadInternet())).isWrongPassword())
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.KeyLocked())).isWrongPassword())
        Assert.assertTrue(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadPassword())).isWrongPassword())
    }

    @Test
    fun testIsKeyServiceError() {
        Assert.assertTrue(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadInternet())).isKeyServiceError())
        Assert.assertTrue(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.KeyLocked())).isKeyServiceError())
        Assert.assertTrue(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadPassword())).isKeyServiceError())
    }
    
    @Test
    fun testIsBiometricError() {
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.SecureStorageFailed(TIMSecureStorageError.FailedToLoadData(Throwable("Test error!")))).isKeyLocked())
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadInternet())).isKeyLocked())
        Assert.assertFalse(TIMStorageError.EncryptedStorageFailed(TIMEncryptedStorageError.KeyServiceFailed(TIMKeyServiceError.BadPassword())).isKeyLocked())
    }

}