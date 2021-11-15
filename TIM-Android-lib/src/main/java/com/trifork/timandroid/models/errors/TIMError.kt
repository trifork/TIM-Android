package com.trifork.timandroid.models.errors

import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import net.openid.appauth.AuthorizationException
import java.lang.IllegalStateException

sealed class TIMError : Throwable()

sealed class TIMAuthError(val sourceError: Throwable) : TIMError() {

    class AuthStateWasNull : TIMAuthError(TODO("MISSING WHAT ERROR SHOULD BE HERE"))

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
    class NoSuitableBrowser(error: Throwable): TIMAuthError(error)
    object NoRegistrationIntentData: TIMAuthError(Exception("No data was found in the registration result intent"))
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

}