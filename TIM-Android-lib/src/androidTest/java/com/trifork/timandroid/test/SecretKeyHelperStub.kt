package com.trifork.timencryptedstorage.test

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.toTIMSuccess
import com.trifork.timencryptedstorage.shared.BiometricCipherConstants
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Secret key helper stub for initializing insecure secret key that does not require user authentication
 */
object SecretKeyHelperStub {
    private const val keyProvider = "AndroidKeyStore"
    private const val keyAlias = "keyAlias"

    //Generate an insecure secret key for testing purposes
    fun createInsecureSecretKey(): TIMResult<SecretKey, TIMEncryptedStorageError> {
        getSecretKey()?.let { return@createInsecureSecretKey it.toTIMSuccess() }

        return generateSecretKey(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BiometricCipherConstants.cipherBlockMode)
                .setEncryptionPaddings(BiometricCipherConstants.cipherPadding)
                .setUserAuthenticationRequired(false)
                .setInvalidatedByBiometricEnrollment(false)
                .build()
        ).toTIMSuccess()
    }

    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            BiometricCipherConstants.cipherAlgorithm, keyProvider
        )
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey? {
        val keyStore = loadKeyStore()
        return keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(keyProvider)
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore
    }
}