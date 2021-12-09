package com.trifork.timandroid.internal

import android.content.Intent
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.TIMAuth
import com.trifork.timandroid.TIMDataStorage
import com.trifork.timandroid.appauth.OpenIDConnectController
import com.trifork.timandroid.models.errors.TIMAuthError
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

internal class TIMAuthInternal(
    private val storage: TIMDataStorage,
    private val openIdController: OpenIDConnectController
) : TIMAuth {

    override fun isLoggedIn(): Boolean = openIdController.isLoggedIn()

    override fun getRefreshToken(): JWT? = openIdController.refreshToken()

    override fun logout() = openIdController.logout()

    override fun handleOpenIDConnectLoginResult(
        scope: CoroutineScope,
        dataIntent: Intent
    ): Deferred<TIMResult<Unit, TIMAuthError>> =
        openIdController.handleLoginIntentResult(scope, dataIntent)

    override fun accessToken(scope: CoroutineScope): Deferred<TIMResult<JWT, TIMError>> =
        openIdController.accessToken(scope, false)

    override fun getOpenIDConnectLoginIntent(scope: CoroutineScope): Deferred<TIMResult<Intent, TIMAuthError>> =
        openIdController.getLoginIntent(scope)

    override fun loginWithPassword(
        scope: CoroutineScope,
        userId: String,
        password: String,
        storeNewRefreshToken: Boolean
    ): Deferred<TIMResult<JWT, TIMError>> = scope.async {
        val storedTokenResult = storage.getStoredRefreshToken(scope, userId, password).await()
        val refreshToken = when (storedTokenResult) {
            is TIMResult.Success -> storedTokenResult.value
            is TIMResult.Failure -> return@async storedTokenResult
        }

        val silentLoginResult = openIdController.silentLogin(scope, refreshToken).await()

        val accessToken = when (silentLoginResult) {
            is TIMResult.Success -> silentLoginResult.value
            is TIMResult.Failure -> return@async silentLoginResult
        }

        return@async if (storeNewRefreshToken) {
            val storedToken = storage.storeRefreshTokenWithExistingPassword(scope, refreshToken, password).await()
            when (storedToken) {
                is TIMResult.Success -> accessToken.toTIMSuccess()
                is TIMResult.Failure -> storedToken
            }
        } else {
            accessToken.toTIMSuccess()
        }
    }

    override fun loginWithBiometricId() {
        TODO("Not yet implemented")
    }
}