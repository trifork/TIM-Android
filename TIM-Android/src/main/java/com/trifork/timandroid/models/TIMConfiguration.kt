package com.trifork.timandroid.models

import android.net.Uri

class TIMConfiguration(
    val timBaseUrl: String,
    val realm: String,
    val clientId: String,
    val redirectUri: Uri,
    val scopes: List<String>,
    // TODO: Add encryption method as constructor param - MFJ
    // TODO: Add key service version param - MFJ
) {


}