package com.trifork.timandroid.models

import android.net.Uri
import android.util.Log
import com.trifork.timandroid.models.openid.TIMOpenIdConnectConfiguration
import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceConfiguration
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceVersion
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage
import java.net.URL

class TIMConfiguration{

    val oidcConfig: TIMOpenIdConnectConfiguration
    val keyServiceConfig: TIMKeyServiceConfiguration
    val encryptionMethod: TIMESEncryptionMethod

    val TAG = "TIMConfiguration"

    constructor(timBaseUrl: URL, realm: String, clientId: String, redirectUri: Uri, scopes: List<String>, encryptionMethod: TIMESEncryptionMethod = TIMESEncryptionMethod.AesGcm, keyServiceVersion: TIMKeyServiceVersion = TIMKeyServiceVersion.V1) {
        Log.d(TAG, timBaseUrl.toURI().rawPath)

        val fullTimUrl = Uri.parse("${timBaseUrl}/auth/realms/$realm")

        this.oidcConfig = TIMOpenIdConnectConfiguration(
            fullTimUrl,
            clientId,
            redirectUri,
            scopes
        )
        //TODO(Get the realmBaseUrl from the fullTimUrl)
        this.keyServiceConfig = TIMKeyServiceConfiguration("${timBaseUrl}/auth/realms/$realm/", keyServiceVersion)
        this.encryptionMethod = encryptionMethod
    }

    /**
     * Combined configuration for AppAuth and TIMEncryptedStorage
     * @param oidcConfig The OIDC configuration
     * @param keyServiceConfig The [TIMKeyService]
     * @param encryptionMethod The encryption method used by TIM (under the hood it's used by [TIMSecureStorage]). Recommended method is [TIMESEncryptionMethod.AesGcm]
     */
    constructor(oidcConfig: TIMOpenIdConnectConfiguration, keyServiceConfig: TIMKeyServiceConfiguration, encryptionMethod: TIMESEncryptionMethod = TIMESEncryptionMethod.AesGcm) {
        this.oidcConfig = oidcConfig
        this.keyServiceConfig = keyServiceConfig
        this.encryptionMethod = encryptionMethod
    }
}

