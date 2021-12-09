package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.helpers.userId
import com.trifork.timandroid.internal.TIMDataStorageInternal
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timandroid.models.errors.TIMStorageError
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.helpers.test.SecureStorageMock
import com.trifork.timencryptedstorage.helpers.test.TIMKeyServiceStub
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TIMStorageInternalTest {

    private val testRefreshToken: JWT = JWT.newInstance("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c")!!

    @Test
    fun testStoreRefreshTokenWithNewPassword() = runBlocking {
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)
        assertFalse(storage.hasRefreshToken(testRefreshToken.userId))
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()
        assertTrue(storage.hasRefreshToken(testRefreshToken.userId))
    }


    @Test
    fun testStoreRefreshTokenWithExistingPassword() = runBlocking {
        val newRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c"
        val updatedRefreshTokenJwt = JWT.newInstance(newRefreshToken)!!
        assertEquals(newRefreshToken.userId, testRefreshToken.userId)

        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)

        // Try to store refresh token with existing password, without having created a new password.
        val storeResult = storage.storeRefreshTokenWithExistingPassword(this, updatedRefreshTokenJwt, "1234").await() as TIMResult.Failure
        val error = storeResult.error as TIMError.Storage
        assertEquals(TIMStorageError.IncompleteUserDataSet::class.java, error.timStorageError::class.java)

        assertFalse(storage.availableUserIds.contains(updatedRefreshTokenJwt.userId))
        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        assertTrue(storage.availableUserIds.contains(updatedRefreshTokenJwt.userId))

        // Store an updated refresh token with an existing password.
        storage.storeRefreshTokenWithExistingPassword(this, updatedRefreshTokenJwt, "1234").await() as TIMResult.Success

        assertTrue(storage.availableUserIds.contains(updatedRefreshTokenJwt.userId)) // Still in the list!
    }

    @Test
    fun testGetRefreshToken() = runBlocking {
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)

        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        assertEquals(TIMResult.Success::class.java, keyModel::class.java)
    }

    //TODO Fails because of missing implementation
    @Test
    fun testBiometricAccessForRefreshToken() = runBlocking {
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)
        val keyModel = timKeyServiceStub.timKeyModel
        // Store refresh token with new password
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        // Enable biometric access
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
        val result = storage.enableBiometricAccessForRefreshToken("1234", testRefreshToken.userId)

        assertEquals(TIMResult.Success::class.java, result::class.java)

        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))

        // Get stored refresh token
        val storedRefreshTokenResult = storage.getStoredRefreshTokenViaBiometric(testRefreshToken.userId) as TIMResult.Success

        assertEquals(testRefreshToken.token, storedRefreshTokenResult.value.refreshToken.token)
        assertEquals(keyModel.longSecret, storedRefreshTokenResult.value.longSecret)

        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
        storage.disableBiometricAccessForRefreshToken(testRefreshToken.userId)
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
    }

    //TODO Fails because of missing implementation
    @Test
    fun testEnableBiometricAccessForRefreshTokenViaLongSecret() = runBlocking {
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)
        val keyModel = timKeyServiceStub.timKeyModel

        // Store refresh token with new password
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        // Enable biometric access
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
        val result = storage.storeRefreshTokenWithLongSecret(testRefreshToken, testRefreshToken.userId)
        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
    }


    //TODO Fails because of missing implementation
    @Test
    fun testStoreRefreshTokenWithLongSecret() = runBlocking {
        val jwt = JWT.newInstance("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c")!!
        val longSecret = "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A="

        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)
        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        storage.storeRefreshTokenWithLongSecret(jwt, longSecret)
        assertTrue(storage.hasRefreshToken(jwt.userId))
    }

    @Test
    fun testClear() = runBlocking {
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)
        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()
        assertTrue(storage.hasRefreshToken(testRefreshToken.userId))

        val result = storage.enableBiometricAccessForRefreshToken("1234", testRefreshToken.userId)

        assertEquals(TIMResult.Success::class.java, result::class.java)

        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))

        storage.clear(testRefreshToken.userId)

        assertEquals(0, storage.availableUserIds.size)
        assertFalse(storage.hasRefreshToken(testRefreshToken.userId))
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
    }

    //TODO Fails because of missing implementation
    @Test
    fun testMultipleUsers() = runBlocking {
        val user1RefreshToken = JWT.newInstance("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.El5bSmm8IPR4M11wg6mMCwnlx2hP7x4XZiaORoTWafY")!!
        val user1Password = "1234"
        val user2RefreshToken = JWT.newInstance("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.q0FBllJKYNGIDEsHj8d0yIGLCaANkyjxER_l1Xm4P50")!!
        val user2Password = "4321"
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)

        assertNotEquals(user1RefreshToken.token, user2RefreshToken.token)
        assertNotEquals(user1RefreshToken.userId, user2RefreshToken.userId)

        // Store refresh tokens with new passwords
        storage.storeRefreshTokenWithNewPassword(this, user1RefreshToken, user1Password).await()
        storage.storeRefreshTokenWithNewPassword(this, user2RefreshToken, user2Password).await()

        assertTrue(storage.hasRefreshToken(user1RefreshToken.userId))
        assertTrue(storage.hasRefreshToken(user2RefreshToken.userId))
        assertEquals(2, storage.availableUserIds.size)

        // Enable bio for user 1
        storage.enableBiometricAccessForRefreshToken(user1Password, user1RefreshToken.userId)
        assertTrue(storage.hasBiometricAccessForRefreshToken(user1RefreshToken.userId))
        assertFalse(storage.hasBiometricAccessForRefreshToken(user2RefreshToken.userId))

        // Get refresh token via bio for user 1
        val bioResult1 = storage.getStoredRefreshTokenViaBiometric(user1RefreshToken.userId) as TIMResult.Success
        assertEquals(user1RefreshToken.token, bioResult1.value.refreshToken.token)

        // Get refresh token via bio for user 2 -> This should fail!
        val bioResult2 = storage.getStoredRefreshTokenViaBiometric(user2RefreshToken.userId) as TIMResult.Failure

        val error = bioResult2.error as TIMError.Storage
        val secureStorageFailed = error.timStorageError as TIMEncryptedStorageError.SecureStorageFailed

        assertEquals(TIMEncryptedStorageError.SecureStorageFailed::class.java, secureStorageFailed::class.java)

        // Get refresh token via password for user 2
        val storageResult = storage.getStoredRefreshToken(this, user2RefreshToken.userId, user2Password).await() as TIMResult.Success
        assertEquals(user2RefreshToken.token, storageResult.value.token)

        // Delete user 2 and check that user 1 is still intact.
        storage.clear(user2RefreshToken.userId)
        assertEquals(1, storage.availableUserIds.size)
        assertFalse(storage.hasRefreshToken(user2RefreshToken.userId))
        assertTrue(storage.hasRefreshToken(user1RefreshToken.userId))
        assertTrue(storage.hasBiometricAccessForRefreshToken(user1RefreshToken.userId))
    }

    //region private helpers

    private val timSecureStorageMock = SecureStorageMock()
    private val timKeyServiceStub = TIMKeyServiceStub()

    private fun dataStorage(encryptionMethod: TIMESEncryptionMethod): TIMDataStorageInternal =
        TIMDataStorageInternal(
            TIMEncryptedStorage(
                timSecureStorageMock,
                timKeyServiceStub,
                encryptionMethod
            )
        )
    
    //endregion
}