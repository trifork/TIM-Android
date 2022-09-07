package com.trifork.timandroid.models.openid

import android.net.Uri
import com.trifork.timandroid.helpers.BiometricRefreshToken
import com.trifork.timandroid.helpers.JWT
import java.net.URL

data class TIMOpenIdConnectConfiguration(
    private val issuerUri: Uri,
    val clientId: String,
    val redirectUri: Uri,
    val scopes: List<String>,
    val additionalParameters: Map<String, String>,
    val prompts: List<String>? = null,
    val backwardSupportConfiguration: BackwardSupportConfiguration? = null) {


    fun getIssuer(refreshToken: JWT? = null): Uri {
        return if (refreshToken?.issuer != null
            && refreshToken.issuer == backwardSupportConfiguration?.issuer?.toString()) {
            backwardSupportConfiguration.issuer
        } else {
            issuerUri
        }
    }
}

data class BackwardSupportConfiguration(val issuer: Uri)