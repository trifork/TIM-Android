package com.trifork.timandroid.models.openid

import android.net.Uri

data class TIMOpenIdConnectConfiguration(
    val issuerUri: Uri,
    val clientId: String,
    val redirectUri: Uri,
    val scopes: List<String>,
    val additionalParameters: Map<String, String>
)