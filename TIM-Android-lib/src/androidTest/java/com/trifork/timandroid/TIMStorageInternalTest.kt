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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TIMStorageInternalTest {

    private val keyServiceBaseUrl = "https://identitymanager.trifork.com"
    private val testRefreshToken: JWT = JWT.newInstance("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c")!!

    @Test
    fun testStoreRefreshTokenWithNewPassword() = runBlocking {
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm)
        assertEquals(false, storage.hasRefreshToken(testRefreshToken.userId))
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()
        assertEquals(true, storage.hasRefreshToken(testRefreshToken.userId))
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

        assertEquals(false, storage.availableUserIds.contains(updatedRefreshTokenJwt.userId))
        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        assertEquals(true, storage.availableUserIds.contains(updatedRefreshTokenJwt.userId))

        // Store an updated refresh token with an existing password.
        storage.storeRefreshTokenWithExistingPassword(this, updatedRefreshTokenJwt, "1234").await() as TIMResult.Success

        assertEquals(true, storage.availableUserIds.contains(updatedRefreshTokenJwt.userId)) // Still in the list!
    }

    private val timSecureStorageMock = SecureStorageMock()

    private fun dataStorage(encryptionMethod: TIMESEncryptionMethod): TIMDataStorageInternal =
        TIMDataStorageInternal(
            TIMEncryptedStorage(
                timSecureStorageMock,
                TIMKeyServiceStub(),
                encryptionMethod
            )
        )
}