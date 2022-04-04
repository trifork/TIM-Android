package com.trifork.timandroid

import android.content.Intent
import androidx.fragment.app.Fragment
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.models.errors.TIMAuthError
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timencryptedstorage.models.TIMResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

interface TIMAuth {

    /**
     * Indicates whether the user has a valid auth state
     */
    fun isLoggedIn(): Boolean

    /**
     * Gets the refresh token from the current session if available
     */
    fun getRefreshToken(): JWT?

    /**
     * Logs out the user of the current session - clearing the auth state (incl. active tokens)
     */
    fun logout()

    /**
     * A blocking version of the [accessToken] function which is useful when adding the accessToken to the headers of a request
     * Gets the current access token from the current session if available
     * This will automatically renew the access token if necessary and if the current refresh token is valid
     *
     */
    fun accessTokenBlocking(): TIMResult<JWT, TIMError>

    /**
     * Gets the current access token from the current session if available
     * This will automatically renew the access token if necessary and if the current refresh token is valid
     */
    fun accessToken(scope: CoroutineScope): Deferred<TIMResult<JWT, TIMError>>

    /**
     * Get a intent for performing a OAuth login with OpenID Connect.
     * Parse the returned intent to a [ActivityResultLauncher] in order to present the OAuth login view.
     * @param authorizationRequestNonce a optional nonce for identifying the login request by knowing which nonce should be present in the resulting JWT. **Use with caution** should always be a randomly generated value.
     */
    fun getOpenIDConnectLoginIntent(scope: CoroutineScope, authorizationRequestNonce: String? = null): Deferred<TIMResult<Intent, TIMAuthError>>

    /**
     * Handles the data received from ChromeCustomTabs. The data can is the returned data in [ActivityResult] as a result of using a [ActivityResultLauncher]
     * @param url The url that was redirected to the app from CustomTabs
     * @return Unit if TIM was able to handle the [url], otherwise TIMAuthError
     */
    fun handleOpenIDConnectLoginResult(scope: CoroutineScope, dataIntent: Intent): Deferred<TIMResult<Unit, TIMAuthError>>

    /**
     * Logs in the user if [userId] and [password] matches.
     * This can only be done if the user has a stored refresh token with a password by calling [getOpenIDConnectLoginIntent]
     * @param userId The userId of the user (can be found in the access token or refresh token)
     * @param storeNewRefreshToken Whether to store the new refresh token. Most use cases will want this set to true
     * @return A result containing the access token if login was successful or a result with an error indicating what went wrong
     */
    fun loginWithPassword(scope: CoroutineScope, userId: String, password: String, storeNewRefreshToken: Boolean): Deferred<TIMResult<JWT, TIMError>>

    /**
     * Logs in using biometric login. This can only be done if the user has stored the refresh token with a password after calling `performOpenIDConnectLogin` AND enabled biometric protection for it.
     * @param scope the coroutine scope. E.g. a view model scope
     * @param userId the userId of the user (can be found in the access token or refresh token)
     * @param storeNewRefreshToken [true] if it should store the new refresh token, and [false] if not. Most people will need this as [true]
     * @param fragment for showing biometric authentication prompt
     */
    fun loginWithBiometricId(scope: CoroutineScope, userId: String, storeNewRefreshToken: Boolean = true, fragment: Fragment): Deferred<TIMResult<JWT, TIMError>>

    /**
     * Enables timeout feature for when the app is in the background. The timeout will clear all current user session data within [TIM]
     * The timeoutHandler will be invoked when the app becomes active, if the app has been in the background longer than the specified duration and the user is logged in
     * The timeout is only triggered if the user is logged in
     * @param durationSeconds The duration in seconds to timeout for. Default is 5 minutes (18000 seconds)
     * @param timeoutHandler A handler
     */
    fun enableBackgroundTimeout(durationSeconds: Long = 18000, timeoutHandler: () -> Unit)

    /**
     * Disables the background timeout
     */
    fun disableBackgroundTimeout()
}