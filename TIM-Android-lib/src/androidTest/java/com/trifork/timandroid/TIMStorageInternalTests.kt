package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.biometric.TIMBiometric
import com.trifork.timandroid.biometric.TIMBiometricData
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.helpers.JWTString
import com.trifork.timandroid.internal.TIMDataStorageInternal
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timandroid.models.errors.TIMStorageError
import com.trifork.timencryptedstorage.TIMEncryptedStorage
import com.trifork.timencryptedstorage.helpers.test.SecretKeyHelperStub
import com.trifork.timencryptedstorage.helpers.test.SecureStorageMock
import com.trifork.timencryptedstorage.helpers.test.TIMKeyServiceStub
import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.keyservice.response.TIMKeyModel
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.shared.BiometricCipherHelper
import com.trifork.timencryptedstorage.shared.SecretKeyHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TIMStorageInternalTests {

    private val loggerStub = mockk<com.trifork.timencryptedstorage.helpers.test.TIMEncryptedStorageLoggerInternal> {
        every { log(any(), any(), any(), any()) } returns Unit
    }

    private val testRefreshToken = JWTHelper("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c")

    @Before
    fun init() {
        //Applies mocking to our SecretKeyHelper object
        mockkObject(SecretKeyHelper)

        //Mocks creation of secret key
        every {
            SecretKeyHelper.getOrCreateSecretKey(any())
        } returns SecretKeyHelperStub.createInsecureSecretKey()

        //Setup Present Biometric Prompt
        //Applies mocking to our TIMBiometric helper object
        mockkObject(TIMBiometric)
        val biometricCipherHelper = BiometricCipherHelper(loggerStub)

        val encryptionCipher = biometricCipherHelper.getInitializedCipherForEncryption("1") as TIMResult.Success // The encryption cipher returned from the biometric prompt
        val decryptionCipher = biometricCipherHelper.getInitializedCipherForDecryption("1", encryptionCipher.value.iv) as TIMResult.Success // The decryption cipher returned from the biometric prompt

        coEvery {
            TIMBiometric.presentBiometricPrompt(any(), any(), any(), any()).await()
        } returns encryptionCipher.value.toTIMSuccess() andThen decryptionCipher.value.toTIMSuccess()
    }

    @Test
    fun testStoreRefreshTokenWithNewPassword() = runBlocking {
        val storage = dataStorage()
        assertFalse(storage.hasRefreshToken(testRefreshToken.userId))
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()
        assertTrue(storage.hasRefreshToken(testRefreshToken.userId))
    }

    @Test
    fun testStoreRefreshTokenWithExistingPassword() = runBlocking {
        val newRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c"
        val updatedRefreshTokenJwt = JWTHelper(newRefreshToken)

        val storage = dataStorage()

        // Try to store refresh token with existing password, without having created a new password.
        val storeResult = storage.storeRefreshTokenWithExistingPassword(this, updatedRefreshTokenJwt, "1234").await() as TIMResult.Failure
        val error = storeResult.error as TIMError.Storage
        assertEquals(TIMStorageError.IncompleteUserDataSet::class, error.timStorageError::class)

        assertFalse(storage.availableUserIds.contains(updatedRefreshTokenJwt.userId))
        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        assertTrue(storage.availableUserIds.contains(updatedRefreshTokenJwt.userId))

        // Store an updated refresh token with an existing password.
        storage.storeRefreshTokenWithExistingPassword(this, updatedRefreshTokenJwt, "1234").await() as TIMResult.Success

        assertTrue(storage.availableUserIds.contains(updatedRefreshTokenJwt.userId)) // Still in the list!
    }

    @Test
    fun testGetRefreshToken() = runBlocking {
        val storage = dataStorage()

        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        assertEquals(TIMResult.Success::class, keyModel::class)
    }

    @Test
    fun testBiometricAccessForRefreshToken() = runBlocking {
        val storage = dataStorage()
        val keyModel = timKeyServiceStub.timKeyModel
        // Store refresh token with new password
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        // Enable biometric access
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))

        val result = storage.enableBiometricAccessForRefreshToken(this, "1234", testRefreshToken.userId, mockk()).await()
        // We assert that biometric access was given successfully
        assertEquals(TIMResult.Success::class, result::class)

        // We assert that we now have biometric access to our refresh token
        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))

        // Get stored refresh token
        val storedRefreshTokenResult = storage.getStoredRefreshTokenViaBiometric(this, testRefreshToken.userId, mockk()).await() as TIMResult.Success

        // The token and long secret match the ones returned by the key server
        assertEquals(testRefreshToken.token, storedRefreshTokenResult.value.refreshToken.token)
        assertEquals(keyModel.longSecret, storedRefreshTokenResult.value.longSecret)

        // We assert that biometric access is disabled by calling disableBiometricAccessForRefreshToken
        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
        storage.disableBiometricAccessForRefreshToken(testRefreshToken.userId)
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
    }

    @Test
    fun testEnableBiometricAccessForRefreshTokenViaLongSecret() = runBlocking {
        val storage = dataStorage()
        val keyModel = timKeyServiceStub.timKeyModel

        // Store refresh token with new password
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()
        // Enable biometric access
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
        val result = storage.enableBiometricAccessForRefreshToken(this, keyModel.longSecret, testRefreshToken.userId, mockk()).await()

        assertEquals(TIMResult.Success::class, result::class)
        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
    }

    @Test
    fun testStoreRefreshTokenWithLongSecret() = runBlocking {
        val jwt = JWTHelper("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.fzHyQ0D6kSOr-6i4gEiJoOm5UutfqgivtqtXbwaRv1c")!!
        val longSecret = "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A="

        val storage = dataStorage()
        val keyModel = storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        storage.storeRefreshTokenWithLongSecret(this, jwt, longSecret).await()
        assertTrue(storage.hasRefreshToken(jwt.userId))
    }

    @Test
    fun testClear() = runBlocking {
        val storage = dataStorage()
        storage.storeRefreshTokenWithNewPassword(this, testRefreshToken, "1234").await()

        assertTrue(storage.hasRefreshToken(testRefreshToken.userId))

        val result = storage.enableBiometricAccessForRefreshToken(this, "1234", testRefreshToken.userId, mockk()).await()

        assertEquals(TIMResult.Success::class, result::class)

        assertTrue(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))

        storage.clear(testRefreshToken.userId)

        assertTrue(storage.availableUserIds.isEmpty())
        assertFalse(storage.hasRefreshToken(testRefreshToken.userId))
        assertFalse(storage.hasBiometricAccessForRefreshToken(testRefreshToken.userId))
    }

    @Test
    fun testMultipleUsers() = runBlocking {
        val user1RefreshToken = JWTHelper("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.El5bSmm8IPR4M11wg6mMCwnlx2hP7x4XZiaORoTWafY")
        val user1Password = "1234"
        val user2RefreshToken = JWTHelper("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk1MTYyMzkwMjJ9.q0FBllJKYNGIDEsHj8d0yIGLCaANkyjxER_l1Xm4P50")
        val user2Password = "4321"
        val storage = dataStorage(TIMESEncryptionMethod.AesGcm, setupKeyServiceSupportingMultipleUsers())

        assertNotEquals(user1RefreshToken.token, user2RefreshToken.token)
        assertNotEquals(user1RefreshToken.userId, user2RefreshToken.userId)

        // Store refresh tokens with new passwords
        storage.storeRefreshTokenWithNewPassword(this, user1RefreshToken, user1Password).await()
        storage.storeRefreshTokenWithNewPassword(this, user2RefreshToken, user2Password).await()

        assertTrue(storage.hasRefreshToken(user1RefreshToken.userId))
        assertTrue(storage.hasRefreshToken(user2RefreshToken.userId))
        assertEquals(2, storage.availableUserIds.size)

        // Enable bio for user 1
        storage.enableBiometricAccessForRefreshToken(this, user1Password, user1RefreshToken.userId, mockk()).await()
        assertTrue(storage.hasBiometricAccessForRefreshToken(user1RefreshToken.userId))
        assertFalse(storage.hasBiometricAccessForRefreshToken(user2RefreshToken.userId))

        // Get refresh token via bio for user 1
        val bioResult1 = storage.getStoredRefreshTokenViaBiometric(this, user1RefreshToken.userId, mockk()).await() as TIMResult.Success
        assertEquals(user1RefreshToken.token, bioResult1.value.refreshToken.token)

        // Get refresh token via bio for user 2 -> This should fail!
        val bioResult2 = storage.getStoredRefreshTokenViaBiometric(this, user2RefreshToken.userId, mockk()).await() as TIMResult.Failure

        val error = bioResult2.error as TIMError.Storage

        assertEquals(TIMStorageError.EncryptedStorageFailed::class, error.timStorageError::class)

        // Get refresh token via password for user 2
        val storageResult = storage.getStoredRefreshToken(this, user2RefreshToken.userId, user2Password).await() as TIMResult.Success
        assertEquals(user2RefreshToken.token, storageResult.value.token)

        // Delete user 2 and check that user 1 is still intact.
        storage.clear(user2RefreshToken.userId)
        assertEquals(1, storage.availableUserIds.size)
        assertFalse(storage.hasRefreshToken(user2RefreshToken.userId))
        assertFalse(storage.hasBiometricAccessForRefreshToken(user2RefreshToken.userId))
        assertTrue(storage.hasRefreshToken(user1RefreshToken.userId))
        assertTrue(storage.hasBiometricAccessForRefreshToken(user1RefreshToken.userId))
    }

    //region private helpers

    private val timSecureStorageMock = SecureStorageMock()
    private val timKeyServiceStub = TIMKeyServiceStub()

    private fun dataStorage(encryptionMethod: TIMESEncryptionMethod = TIMESEncryptionMethod.AesGcm, timKeyServiceStub: TIMKeyService = TIMKeyServiceStub()): TIMDataStorageInternal =
        TIMDataStorageInternal(
            TIMEncryptedStorage(
                timSecureStorageMock,
                loggerStub,
                timKeyServiceStub,
                encryptionMethod
            ),
            TIMBiometricData.Builder().build()
        )

    private fun setupKeyServiceSupportingMultipleUsers(): TIMKeyService {
        val timKeyModelOne = TIMKeyModel(
            "168dfa8a-a613-488d-876c-1a79122c8d5a",
            "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK9o=",
            "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3A="
        )

        val timKeyModelTwo = TIMKeyModel(
            "168dfa8a-a613-488d-876c-1a79122c8sda",
            "/RT5VXFinR27coWdsieCt3UxoKibplkO+bCVNkDJK1o=",
            "xe6XhucZ0BnH3yLQFR1wrZgPe3l4q/ymnQCCY/iZs3B="
        )

        return mockk {
            //The first key is timKeyModelOne, next timKeyModelTwo
            coEvery {
                createKey(any(), any()).await()
            } returns timKeyModelOne.toTIMSuccess() andThen timKeyModelTwo.toTIMSuccess()

            //In case timKeyModelOne.keyId is used to get a new secret key, return KeyModelOne
            coEvery {
                getKeyViaSecret(any(), any(), timKeyModelOne.keyId).await()
            } returns timKeyModelOne.toTIMSuccess()

            //In case timKeyModelOne.keyId is used to get a new secret key, return KeyModelOne
            coEvery {
                getKeyViaLongSecret(any(), any(), timKeyModelOne.keyId).await()
            } returns timKeyModelOne.toTIMSuccess()

            //In case timKeyModelTwo.keyId is used to get a new secret key, return KeyModelTwo
            coEvery {
                getKeyViaSecret(any(), any(), timKeyModelTwo.keyId).await()
            } returns timKeyModelTwo.toTIMSuccess()

            //In case timKeyModelTwo.keyId is used to get a new secret key, return KeyModelTwo
            coEvery {
                getKeyViaLongSecret(any(), any(), timKeyModelTwo.keyId).await()
            } returns timKeyModelTwo.toTIMSuccess()
        }
    }

    private fun JWTHelper(jwtString: JWTString): JWT {
        val jwtResult = JWT.newInstance(jwtString) as TIMResult.Success
        return jwtResult.value
    }

    //endregion
}