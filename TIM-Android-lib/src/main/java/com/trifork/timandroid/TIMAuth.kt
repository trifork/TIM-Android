package com.trifork.timandroid

import android.content.Intent
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

    // TODO: Potentially use JWT lib to decode for values - MFJ (20/09/2021)
    /**
     * Gets the refresh token from the current session if available
     */
    fun getRefreshToken(): JWT?

    /**
     * Logs out the user of the current session - clearing the auth state (incl. active tokens)
     */
    fun logout()

    /**
     * Gets the current access token from the current session if available
     * This will automatically renew the access token if necessary and if the current refresh token is valid
     */
    fun accessToken(): TIMResult<JWT, TIMError>

    /**
     * // TODO: Missing docs - MFJ (20/09/2021)
     */
    fun getOpenIDConnectLoginIntent(scope: CoroutineScope): Deferred<TIMResult<Intent, TIMAuthError>>

    /**
     * Handles redirect from ChromeCustomTabs.
     * @param url The url that was redirected to the app from CustomTabs
     * @return True if TIM was able to handle the [url], otherwise false
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
     * // TODO: Missing docs - MFJ (20/09/2021)
     */
    fun loginWithBiometricId()
}