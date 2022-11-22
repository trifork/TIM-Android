package com.trifork.timandroid.appauth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.models.errors.TIMAuthError
import com.trifork.timandroid.models.openid.TIMOpenIdConnectConfiguration
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import net.openid.appauth.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AppAuthController(
    private val config: TIMOpenIdConnectConfiguration,
    context: Context
) : OpenIDConnectController {

    private val authorizationService = AuthorizationService(
        context.applicationContext,
        AppAuthConfiguration.DEFAULT
    )

    private var authState: AuthState? = null

    override fun isLoggedIn(): Boolean = authState != null

    override fun getLoginIntent(scope: CoroutineScope, authorizationRequestNonce: String?): Deferred<TIMResult<Intent, TIMAuthError>> =
        scope.async {
            val serviceConfigResult = discoverConfiguration(scope).await()
            when (serviceConfigResult) {
                is TIMResult.Success -> {
                    val authRequest = buildAuthRequest(serviceConfigResult.value, authorizationRequestNonce)
                    try {
                        authorizationService.getAuthorizationRequestIntent(authRequest)
                            .toTIMSuccess()
                    } catch (e: ActivityNotFoundException) {
                        TIMAuthError.NoSuitableBrowser(e).toTIMFailure()
                    }
                }
                is TIMResult.Failure -> serviceConfigResult
            }
        }

    override fun handleLoginIntentResult(
        scope: CoroutineScope,
        dataIntent: Intent?
    ): Deferred<TIMResult<Unit, TIMAuthError>> = scope.async {
        dataIntent ?: return@async TIMAuthError.NoRegistrationIntentData.toTIMFailure()

        val response = AuthorizationResponse.fromIntent(dataIntent)
        val error = AuthorizationException.fromIntent(dataIntent)

        val authResponse = handleAppAuthCallback(
            response,
            error,
            fallbackError = TIMAuthError.AppAuthFailed("No valid response or error encountered when unwrapping registration result intent data")
        )

        when (authResponse) {
            is TIMResult.Success -> {
                val tokenRequest: TIMResult<TokenResponse, TIMAuthError> = performTokenRequest(
                    this,
                    authResponse.value.createTokenExchangeRequest()
                ).await()

                when (tokenRequest) {
                    is TIMResult.Success -> {
                        authState = AuthState(authResponse.value, tokenRequest.value, error)
                        Unit.toTIMSuccess()
                    }
                    is TIMResult.Failure -> tokenRequest
                }
            }
            is TIMResult.Failure -> return@async authResponse
        }
    }

    override fun silentLogin(
        scope: CoroutineScope,
        refreshToken: JWT
    ): Deferred<TIMResult<JWT, TIMAuthError>> = scope.async {
        val discoverServiceConfigResult = discoverConfiguration(scope).await()
        val discoveredServiceConfig = when (discoverServiceConfigResult) {
            is TIMResult.Success -> discoverServiceConfigResult.value
            is TIMResult.Failure -> return@async discoverServiceConfigResult
        }

        val tokenRequest = TokenRequest.Builder(
            discoverServiceConfigResult.value,
            config.clientId
        )
            .setScopes(config.scopes)
            .setAdditionalParameters(config.additionalParameters)
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setRefreshToken(refreshToken.token)
            .build()

        val tokenResponseResult = performTokenRequest(scope, tokenRequest).await()

        val newAuthState = when (tokenResponseResult) {
            is TIMResult.Success -> {
                AuthState(
                    buildAuthorizationResponseFromConfig(discoveredServiceConfig),
                    tokenResponseResult.value,
                    null
                )
            }
            is TIMResult.Failure -> return@async tokenResponseResult
        }.apply {
            authState = this
        }

        val newAuthStateAccessToken = newAuthState.accessToken

        if (newAuthStateAccessToken != null) {
            val newJWTResult = JWT.newInstance(newAuthStateAccessToken)
            return@async when(newJWTResult) {
                is TIMResult.Failure -> TIMAuthError.FailedToGetRequiredDataInToken(newJWTResult.error).toTIMFailure()
                is TIMResult.Success -> newJWTResult
            }
        }
        return@async TIMAuthError.FailedToGetAccessToken.toTIMFailure()
    }

    private fun buildAuthorizationResponseFromConfig(serviceConfiguration: AuthorizationServiceConfiguration) =
        AuthorizationResponse.Builder(
            buildAuthRequest(serviceConfiguration)
        ).build()

    override fun accessToken(scope: CoroutineScope, forceRefresh: Boolean): Deferred<TIMResult<JWT, TIMAuthError>> = scope.async {
        if (authState == null) {
            return@async TIMAuthError.AuthStateWasNull().toTIMFailure()
        }

        val freshTokenResult = performActionWithFreshTokens(scope, forceRefresh).await()

        return@async when (freshTokenResult) {
            is TIMResult.Failure -> freshTokenResult.error.toTIMFailure()
            is TIMResult.Success -> {
                val newJWT = JWT.newInstance(freshTokenResult.value)
                when (newJWT) {
                    is TIMResult.Failure -> TIMAuthError.FailedToGetRequiredDataInToken(newJWT.error).toTIMFailure()
                    is TIMResult.Success -> newJWT
                }
            }
        }
    }

    override fun refreshToken(): JWT? {
        val refreshToken = authState?.refreshToken
        return if (refreshToken != null) {
            val jwt = JWT.newInstance(refreshToken)
            if(jwt is TIMResult.Success) jwt.value else null
        } else {
            null
        }
    }

    override fun logout() {
        authState = null
    }

    /**
     * Builds a AuthorizationRequest, adds the authorizationRequestNonce in case it is not null
     */
    private fun buildAuthRequest(authServiceConfig: AuthorizationServiceConfiguration, authorizationRequestNonce: String? = null): AuthorizationRequest =
        AuthorizationRequest.Builder(
            authServiceConfig,
            config.clientId,
            ResponseTypeValues.CODE,
            config.redirectUri
        )
            .setScopes(config.scopes)
            .setAdditionalParameters(config.additionalParameters)
            .also {
                if(authorizationRequestNonce != null) {
                    it.setNonce(authorizationRequestNonce)
                }
                if (!config.prompts.isNullOrEmpty()) {
                    it.setPromptValues(config.prompts)
                }
            }
            .build()

    /**
     * Attempts to retrieve a fresh accessToken
     * @return a fresh OpenID accessToken
     */
    private fun performActionWithFreshTokens(
        scope: CoroutineScope,
        forceRefresh: Boolean
    ): Deferred<TIMResult<String, TIMAuthError>> =
        scope.async {
            suspendCoroutine { continuation ->
                if (forceRefresh) {
                    authState?.needsTokenRefresh = true
                }
                authState?.performActionWithFreshTokens(authorizationService) { accessToken, _, error ->
                    continuation.resume(
                        handleAppAuthCallback(
                            accessToken,
                            error,
                            TIMAuthError.FailedToGetAccessToken
                        )
                    )
                }
            }
        }

    /**
     * Attempts to discover the [AuthorizationServiceConfiguration] for [TIMOpenIdConnectConfiguration.issuerUri].
     * This implementation uses [AppAuthController.config] passed on creation of this class
     * @return The service configuration for the given OpenID Connect issuer
     */
    private fun discoverConfiguration(scope: CoroutineScope): Deferred<TIMResult<AuthorizationServiceConfiguration, TIMAuthError>> =
        scope.async {
            suspendCoroutine { continuation ->
                AuthorizationServiceConfiguration.fetchFromIssuer(config.issuerUri) { configuration, error ->
                    continuation.resume(
                        handleAppAuthCallback(
                            configuration,
                            error,
                            TIMAuthError.FailedToDiscoverConfiguration(null)
                        )
                    )
                }
            }
        }

    private fun performTokenRequest(
        scope: CoroutineScope,
        tokenRequest: TokenRequest
    ): Deferred<TIMResult<TokenResponse, TIMAuthError>> = scope.async {
        suspendCoroutine { continuation ->
            authorizationService.performTokenRequest(tokenRequest) { tokenResponse: TokenResponse?, exception: AuthorizationException? ->
                continuation.resume(
                    handleAppAuthCallback(
                        tokenResponse,
                        exception,
                        TIMAuthError.AppAuthFailed("Unknown error when attempting to call performTokenRequest during registration result handling")
                    )
                )
            }
        }
    }

    private fun <T> handleAppAuthCallback(value: T?, error: AuthorizationException?, fallbackError: TIMAuthError): TIMResult<T, TIMAuthError> =
        when {
            error != null -> TIMAuthError.mapAppAuthError(error)
            value != null -> value.toTIMSuccess()
            else -> fallbackError.toTIMFailure()
        }

}