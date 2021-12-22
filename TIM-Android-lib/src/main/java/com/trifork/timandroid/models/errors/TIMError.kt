package com.trifork.timandroid.models.errors

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

    //TODO MISSING WHAT ERROR SHOULD BE HERE
    class AuthStateWasNull : TIMAuthError()


    //region AccessToken
    //TODO Which errors do we push for the AccessToken related errors?
    class FailedToGetAccessToken : TIMAuthError()
    class FailedToGetRequiredDataInToken : TIMAuthError()
    //endregion

    class AppAuthNetworkError(error: Throwable) : TIMAuthError(error)
    class AppAuthFailed(error: Throwable) : TIMAuthError(error) {
        constructor(unknownErrorMessage: String) : this(UnknownError(unknownErrorMessage))
    }

    //region Service Discovery
    class FailedToDiscoverConfiguration(error: Throwable?) :
        TIMAuthError(error ?: UnknownError("Failed to get discover result and error was null"))
    //endregion

    class RefreshTokenExpired(error: Throwable) : TIMAuthError(error)

    //region Registration
    class NoSuitableBrowser(error: Throwable) : TIMAuthError(error)
    object NoRegistrationIntentData : TIMAuthError(Exception("No data was found in the registration result intent"))
    //endregion

    companion object {
        fun mapAppAuthError(error: AuthorizationException): TIMResult.Failure<TIMAuthError> =
            when (error.type) {
                AuthorizationException.TYPE_GENERAL_ERROR ->
                    when (error.code) {
                        AuthorizationException.GeneralErrors.NETWORK_ERROR.code -> AppAuthNetworkError(
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

    fun isKeyLocked(): Boolean =
        isKeyServiceErrorInternal(TIMKeyServiceError.KeyLocked())

    fun isWrongPassword(): Boolean =
        isKeyServiceErrorInternal(TIMKeyServiceError.BadPassword())

    /**
     *   Determines whether this error is an error thrown by the KeyService.
     *
     *   This might be useful for handling unexpected cases from the encryption part of the framework.
     *   When the key service fails you don't want to do any drastic fallback, since the server might "just" be down or the user have no internet connection. You will be able to recover later on, from a key service error.
     **/
    //TODO Do we actually need this? Or can we let others call isKeyServiceError below directly?
    fun isKeyServiceError() : Boolean = isKeyServiceErrorInternal()

    /**
     * Determines whether this error is a specific kind of key service error.
     * @param keyServiceError The key service error to look for. If [null] is passed it will look for any kind of key service error.
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
        }

    //TODO Can we use this as is? (taken directly from iOS sdk?) - JHE 21.12.21
    fun isBiometricFailedError() : Boolean =
        when (this) {
            is EncryptedStorageFailed -> {
                when (this.timEncryptedStorageError) {
                    is TIMEncryptedStorageError.SecureStorageFailed -> true
                    else -> false
                }
            }
            is IncompleteUserDataSet -> false
        }
}

sealed class TIMBiometricError: TIMError() {

    //region Registration
    class BiometricAuthenticationError(error: Throwable) : TIMBiometricError()
    //endregion

}