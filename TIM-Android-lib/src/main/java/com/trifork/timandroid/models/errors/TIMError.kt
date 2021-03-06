@file:Suppress("unused")

package com.trifork.timandroid.models.errors

import androidx.biometric.BiometricPrompt
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.toTIMFailure
import net.openid.appauth.AuthorizationException

sealed class TIMError : Throwable() {
    class Auth(val timAuthError: TIMAuthError) : TIMError()
    class Storage(val timStorageError: TIMStorageError) : TIMError()
}

sealed class TIMAuthError(val sourceError: Throwable? = null) : TIMError() {

    class AuthStateWasNull : TIMAuthError()
    class FailedToValidateIDToken(val error: Throwable) : TIMAuthError(error)

    //region AccessToken
    object FailedToGetAccessToken : TIMAuthError()
    object FailedToGetRefreshToken : TIMAuthError()
    class FailedToGetRequiredDataInToken(val timEncryptedStorageError: TIMEncryptedStorageError) : TIMAuthError()
    //endregion

    class AppAuthNetworkError(val error: Throwable) : TIMAuthError(error)
    class AppAuthFailed(val error: Throwable) : TIMAuthError(error) {
        constructor(unknownErrorMessage: String) : this(UnknownError(unknownErrorMessage))
    }

    //region Service Discovery
    class FailedToDiscoverConfiguration(val error: Throwable?) :
        TIMAuthError(error ?: UnknownError("Failed to get discover result and error was null"))
    //endregion

    class RefreshTokenExpired(val error: Throwable) : TIMAuthError(error)

    //region Registration
    class NoSuitableBrowser(val error: Throwable) : TIMAuthError(error)
    object NoRegistrationIntentData : TIMAuthError(Exception("No data was found in the registration result intent"))
    //endregion

    fun isRefreshTokenExpiredError(): Boolean = this::class == RefreshTokenExpired::class

    companion object {
        fun mapAppAuthError(error: AuthorizationException): TIMResult.Failure<TIMAuthError> =
            when (error.type) {
                AuthorizationException.TYPE_GENERAL_ERROR ->
                    when (error.code) {
                        AuthorizationException.GeneralErrors.NETWORK_ERROR.code -> AppAuthNetworkError(
                            error
                        )
                        AuthorizationException.GeneralErrors.ID_TOKEN_VALIDATION_ERROR.code -> FailedToValidateIDToken(
                            error
                        )
                        else -> AppAuthFailed(error)
                    }
                AuthorizationException.TYPE_OAUTH_TOKEN_ERROR ->
                    when (error.code) {
                        AuthorizationException.TokenRequestErrors.INVALID_GRANT.code -> RefreshTokenExpired(
                            error
                        )
                        else -> AppAuthFailed(error)
                    }
                else -> AppAuthFailed(error)
            }.toTIMFailure()
    }
}

sealed class TIMStorageError : TIMError() {
    class EncryptedStorageFailed(val timEncryptedStorageError: TIMEncryptedStorageError) : TIMStorageError()
    class IncompleteUserDataSet : TIMStorageError()

    //region Biometric errors
    class BiometricAuthenticationError(val errorCode: Int?, val error: Throwable) : TIMStorageError()
    //endregion

    /**
     * Determines whether this error means the key is locked because of too many incorrect password attempts
     * In which case we would need the user to authenticate again
     */
    fun isKeyLocked(): Boolean =
        isKeyServiceErrorInternal(TIMKeyServiceError.KeyLocked())

    /**
     * Determines whether this error means the password was incorrect
     */
    fun isWrongPassword(): Boolean =
        isKeyServiceErrorInternal(TIMKeyServiceError.BadPassword())

    /**
     * Determines whether this error means that fingerprints were added or completely removed
     * In which case we would need the user to authenticate again and re-enable biometric authentication
     */
    fun isBiometricAuthenticationInvalidated(): Boolean =
        when (this) {
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.PermanentlyInvalidatedKey -> true
                    else -> false
                }
            }
            else -> {
                false
            }
        }

    /**
     * Determines whether this is an biometric authentication unrecoverable error
     * This state should not be common for any application but was achieved on a single device using a beta version of android 12.
     * Please report findings of this error.
     */
    fun isBiometricAuthenticationUnrecoverable(): Boolean =
        when (this) {
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.UnrecoverablyFailedToEncrypt -> true
                    is TIMEncryptedStorageError.UnrecoverablyFailedToDecrypt -> true
                    else -> false
                }
            }
            else -> {
                false
            }
        }

    /**
     * Determines whether this error is an error thrown by the KeyService.
     *
     * This might be useful for handling unexpected cases from the encryption part of the framework.
     * When the key service fails you don't want to do any drastic fallback, since the server might "just" be down or the user have no internet connection. You will be able to recover later on, from a key service error.
     **/
    fun isKeyServiceError(): Boolean = isKeyServiceErrorInternal()

    /**
     * Determines whether this error is a specific kind of key service error.
     * @param keyServiceError The key service error to look for. If null is passed it will look for any kind of key service error.
     */
    private fun isKeyServiceErrorInternal(keyServiceError: TIMKeyServiceError? = null): Boolean =
        when (this) {
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.KeyServiceFailed -> {
                        if (keyServiceError != null) {
                            this.timEncryptedStorageError.error::class == keyServiceError::class
                        } else {
                            true
                        }
                    }
                    else -> {
                        false
                    }
                }
            }
            is IncompleteUserDataSet -> false
            is BiometricAuthenticationError -> false
        }

    //TODO Is this correct? (BiometricAuthenticationError is not present in iOS sdk) - JHE 21.12.21
    /**
     * Determines whether the thrown error is caused by the biometric authentication failing.
     * Biometric authentication has failed if the child exception is either a [BiometricAuthenticationError] or a [TIMEncryptedStorageError.SecureStorageFailed] error.
     */
    fun isBiometricFailedError(): Boolean =
        when (this) {
            is BiometricAuthenticationError -> true
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.SecureStorageFailed -> true
                    else -> false
                }
            }
            is IncompleteUserDataSet -> false
        }

    //TODO Added this, should it be on Auth error like iOS .isSafariViewControllerCancelled error?
    /**
     * Determines whether the thrown error is a biometric canceled error, is either thrown by the user actively canceling the biometric authentication prompt or the android system closing it when the application is send to the background
     * In that case: Ignore the error.
     */
    fun isBiometricCanceledError(): Boolean =
        when (this) {
            is BiometricAuthenticationError -> this.errorCode == BiometricPrompt.ERROR_CANCELED || this.errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || this.errorCode == BiometricPrompt.ERROR_USER_CANCELED
            else -> false
        }

    //region Registration
}