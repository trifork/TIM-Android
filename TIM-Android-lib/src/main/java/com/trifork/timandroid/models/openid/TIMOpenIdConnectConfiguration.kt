package com.trifork.timandroid.models.openid

import android.net.Uri

data class TIMOpenIdConnectConfiguration(
    val issuerUrl: Uri,
    val clientId: String,
    val redirectUri: Uri,
    val scopes: List<String>
)