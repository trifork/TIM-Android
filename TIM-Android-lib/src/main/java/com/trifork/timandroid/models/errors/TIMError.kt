@file:Suppress("unused")

package com.trifork.timandroid.models.errors

import androidx.biometric.BiometricPrompt
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.errors.TIMKeyServiceError
import com.trifork.timencryptedstorage.models.toTIMFailure
import net.openid.appauth.AuthorizationException

public sealed class TIMError : Throwable() {
    public class Auth(
        public val timAuthError: TIMAuthError
    ) : TIMError()

    public class Storage(
        public val timStorageError: TIMStorageError
    ) : TIMError()
}

public sealed class TIMAuthError(
    public val sourceError: Throwable? = null
) : TIMError() {

    public class AuthStateWasNull : TIMAuthError()
    public class FailedToValidateIDToken(
        public val error: Throwable
    ) : TIMAuthError(error)

    public data object FailedToGetAccessToken : TIMAuthError()
    public data object FailedToGetRefreshToken : TIMAuthError()
    public class FailedToGetRequiredDataInToken(
        public val timEncryptedStorageError: TIMEncryptedStorageError
    ) : TIMAuthError()

    public class AppAuthNetworkError(
        public val error: Throwable
    ) : TIMAuthError(error)

    public class AppAuthFailed(
        public val error: Throwable
    ) : TIMAuthError(error) {
        public constructor(unknownErrorMessage: String) : this(UnknownError(unknownErrorMessage))
    }

    public class FailedToDiscoverConfiguration(
        public val error: Throwable?
    ) : TIMAuthError(error ?: UnknownError("Failed to get discover result and error was null"))

    public class RefreshTokenExpired(
        public val error: Throwable
    ) : TIMAuthError(error)

    public class NoSuitableBrowser(
        public val error: Throwable
    ) : TIMAuthError(error)

    public object NoRegistrationIntentData : TIMAuthError(
        sourceError = Exception("No data was found in the registration result intent")
    )

    public fun isRefreshTokenExpiredError(): Boolean = this is RefreshTokenExpired

    public companion object {
        public fun mapAppAuthError(
            error: AuthorizationException
        ): TIMResult.Failure<TIMAuthError> =
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

public sealed class TIMStorageError : TIMError() {
    public class EncryptedStorageFailed(
        public val timEncryptedStorageError: TIMEncryptedStorageError
    ) : TIMStorageError()

    public class IncompleteUserDataSet : TIMStorageError()

    public class BiometricAuthenticationError(
        public val errorCode: Int?,
        public val error: Throwable
    ) : TIMStorageError()

    /**
     * Determines whether this error means the key is locked because of too many incorrect password attempts
     * In which case we would need the user to authenticate again
     */
    public fun isKeyLocked(): Boolean =
        isKeyServiceErrorInternal(TIMKeyServiceError.KeyLocked())

    /**
     * Determines whether this error means the password was incorrect
     */
    public fun isWrongPassword(): Boolean =
        isKeyServiceErrorInternal(TIMKeyServiceError.BadPassword())

    /**
     * Determines whether this error means that fingerprints were added or completely removed
     * In which case we would need the user to authenticate again and re-enable biometric authentication
     */
    //TODO move this logic into the various classes as this is easy to break.
    public fun isBiometricAuthenticationInvalidated(): Boolean =
        when (this) {
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.PermanentlyInvalidatedKey -> true
                    else -> false
                }
            }

            is BiometricAuthenticationError -> false
            is IncompleteUserDataSet -> false
        }

    /**
     * Determines whether this is an biometric authentication unrecoverable error
     * This state should not be common for any application but was achieved on a single device using a beta version of android 12.
     * Please report findings of this error.
     */
    //TODO move this logic into the various classes as this is easy to break.
    public fun isBiometricAuthenticationUnrecoverable(): Boolean =
        when (this) {
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.UnrecoverablyFailedToEncrypt -> true
                    is TIMEncryptedStorageError.UnrecoverablyFailedToDecrypt -> true
                    else -> false
                }
            }

            is BiometricAuthenticationError -> false
            is IncompleteUserDataSet -> false
        }

    /**
     * Determines whether this error is an error thrown by the KeyService.
     *
     * This might be useful for handling unexpected cases from the encryption part of the framework.
     * When the key service fails you don't want to do any drastic fallback, since the server might "just" be down or the user have no internet connection. You will be able to recover later on, from a key service error.
     **/
    public fun isKeyServiceError(): Boolean = isKeyServiceErrorInternal()

    /**
     * Determines whether this error is a specific kind of key service error.
     * @param keyServiceError The key service error to look for. If null is passed it will look for any kind of key service error.
     */
    //TODO move this logic into the various classes as this is easy to break.

    private fun isKeyServiceErrorInternal(
        keyServiceError: TIMKeyServiceError? = null
    ): Boolean = when (this) {
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
    //TODO move this logic into the various classes as this is easy to break.
    public fun isBiometricFailedError(): Boolean = when (this) {
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
    //TODO move this logic into the various classes as this is easy to break.
    public fun isBiometricCanceledError(): Boolean =
        when (this) {
            is BiometricAuthenticationError -> this.errorCode == BiometricPrompt.ERROR_CANCELED || this.errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || this.errorCode == BiometricPrompt.ERROR_USER_CANCELED
            else -> false
        }

}