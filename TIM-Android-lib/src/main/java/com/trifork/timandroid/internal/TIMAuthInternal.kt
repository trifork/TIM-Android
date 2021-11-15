package com.trifork.timandroid.internal

import android.content.Intent
import com.trifork.timandroid.JWT
import com.trifork.timandroid.TIMAuth
import com.trifork.timandroid.TIMDataStorage
import com.trifork.timandroid.appauth.OpenIDConnectController
import com.trifork.timandroid.models.errors.TIMAuthError
import com.trifork.timandroid.models.errors.TIMError
import com.trifork.timencryptedstorage.models.TIMResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

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

    override fun accessToken(): TIMResult<JWT, TIMError> =
        openIdController.accessToken(forceRefresh = false)

    override fun getOpenIDConnectLoginIntent(scope: CoroutineScope): Deferred<TIMResult<Intent, TIMAuthError>> =
        openIdController.getLoginIntent(scope)

    override fun loginWithPassword(
        scope: CoroutineScope,
        userId: String,
        password: String,
        storeNewRefreshToken: Boolean
    ): TIMResult<JWT, TIMError> {
        val storedTokenResult = storage.getStoredRefreshToken(userId, password)
        val refreshToken = when(storedTokenResult) {
            is TIMResult.Success -> storedTokenResult.value
            is TIMResult.Failure -> return storedTokenResult
        }

        val silentLoginResult = openIdController.silentLogin(refreshToken)

        val newRefreshToken = when(silentLoginResult) {
            is TIMResult.Success -> silentLoginResult.value
            is TIMResult.Failure -> return silentLoginResult
        }

        if(storeNewRefreshToken) {

        }
        TODO("Not yet implemented")
    }

    override fun loginWithBiometricId() {
        TODO("Not yet implemented")
    }
}