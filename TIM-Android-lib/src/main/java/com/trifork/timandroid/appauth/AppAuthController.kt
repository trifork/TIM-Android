package com.trifork.timandroid.appauth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.models.errors.TIMAuthError
import com.trifork.timandroid.models.openid.TIMOpenIdConnectConfiguration
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSucces
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

    override fun getLoginIntent(scope: CoroutineScope): Deferred<TIMResult<Intent, TIMAuthError>> =
        scope.async {
            val serviceConfigResult = discoverConfiguration(scope).await()
            when (serviceConfigResult) {
                is TIMResult.Success -> {
                    val authRequest = buildAuthRequest(serviceConfigResult.value)
                    try {
                        authorizationService.getAuthorizationRequestIntent(authRequest)
                            .toTIMSucces()
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

        val authReponse = handleAppAuthCallback(
            response,
            error,
            fallbackError = TIMAuthError.AppAuthFailed("No valid response or error encountered when unwrapping registration result intent data")
        )

        when (authReponse) {
            is TIMResult.Success -> {
                val tokenRequest: TIMResult<TokenResponse, TIMAuthError> = performTokenRequest(
                    this,
                    authReponse.value.createTokenExchangeRequest()
                ).await()

                when (tokenRequest) {
                    is TIMResult.Success -> {
                        authState = AuthState(authReponse.value, tokenRequest.value, error)
                        Unit.toTIMSucces()
                    }
                    is TIMResult.Failure -> tokenRequest
                }
            }
            is TIMResult.Failure -> return@async authReponse
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


        newAuthState.refreshToken.toTIMSucces()



        TODO("Not yet implemented")
    }

    private fun buildAuthorizationResponseFromConfig(serviceConfiguration: AuthorizationServiceConfiguration) =
        AuthorizationResponse.Builder(
            buildAuthRequest(serviceConfiguration)
        ).build()

    override fun accessToken(forceRefresh: Boolean): TIMResult<JWT, TIMAuthError> {
        TODO("Not yet implemented")
    }

    override fun refreshToken(): JWT? {
        val refreshToken = authState?.refreshToken
        return if (refreshToken != null) {
            JWT.newInstance(refreshToken)
        } else {
            null
        }
    }

    override fun logout() {
        authState = null
    }

    /**
     *
     */
    private fun buildAuthRequest(authServiceConfig: AuthorizationServiceConfiguration): AuthorizationRequest =
        AuthorizationRequest.Builder(
            authServiceConfig,
            config.clientId,
            ResponseTypeValues.CODE,
            config.redirectUri
        )
            .setScopes(config.scopes)
            .build()


    /**
     * Attempts to discover the [AuthorizationServiceConfiguration] for [TIMOpenIdConnectConfiguration.issuerUrl].
     * This implementation uses [AppAuthController.config] passed on creation of this class
     * @return The service configuration for the given OpenID Connect issuer
     */
    private fun discoverConfiguration(scope: CoroutineScope): Deferred<TIMResult<AuthorizationServiceConfiguration, TIMAuthError>> =
        scope.async {
            suspendCoroutine { continuation ->
                AuthorizationServiceConfiguration.fetchFromIssuer(config.issuerUrl) { configuration, error ->
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
            value != null -> value.toTIMSucces()
            else -> fallbackError.toTIMFailure()
        }

}