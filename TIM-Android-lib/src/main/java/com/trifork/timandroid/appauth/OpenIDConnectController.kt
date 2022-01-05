package com.trifork.timandroid.appauth

import android.app.Activity
import android.content.Intent
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.models.errors.TIMAuthError
import com.trifork.timencryptedstorage.models.TIMResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

/**
 * Controller for the OpenID Connect dependency
 */
interface OpenIDConnectController {

    /**
     * Checks whether there is a valid OpenID Connect session stored
     */
    fun isLoggedIn(): Boolean

    /**
     * Creates an intent for signing in to the Open ID Connect service
     * @return The intent to use for [Activity.startActivityForResult]
     */
    fun getLoginIntent(scope: CoroutineScope): Deferred<TIMResult<Intent, TIMAuthError>>

    /**
     * Handles result originating from the Intent from [getLoginIntent] and attempts to register for a valid user session
     * @return The users access token or an error if something went wrong
     */
    fun handleLoginIntentResult(scope: CoroutineScope, dataIntent: Intent?): Deferred<TIMResult<Unit, TIMAuthError>>

    /**
     * Perform a login with an existing [refreshToken], thereby skipping the main Open ID Connect flow
     * @return a updated JWT
     */
    fun silentLogin(scope: CoroutineScope, refreshToken: JWT): Deferred<TIMResult<JWT, TIMAuthError>>

    /**
     * Gets existing or retrieves a updated JWT using app auth.
     * Use this function to get the access token instead of storing it in a local variable.
     * Making sure you have the latest accessToken at all times.
     * @return a updated JWT
     */
    fun accessToken(scope: CoroutineScope, forceRefresh: Boolean): Deferred<TIMResult<JWT, TIMAuthError>>

    /**
     * Get the refresh token
     */
    fun refreshToken(): JWT?

    /**
     * Clears the current state for the logged in user
     */
    fun logout()
}