package com.trifork.timandroid.models

import android.net.Uri
import com.trifork.timandroid.models.openid.BackwardSupportConfiguration
import com.trifork.timandroid.models.openid.TIMOpenIdConnectConfiguration
import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceConfiguration
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceVersion
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage
import java.net.URL

/**
 * Combined configuration for AppAuth and TIMEncryptedStorage
 */
class TIMConfiguration {

    /**
     * OIDC configuration
     */
    val oidcConfig: TIMOpenIdConnectConfiguration

    /**
     * KeyService configuration
     */
    val keyServiceConfig: TIMKeyServiceConfiguration

    /**
     * The encryption method used by TIM, [TIMESEncryptionMethod.AesGcm] is the default and only supported as of now
     */
    val encryptionMethod: TIMESEncryptionMethod

    /**
     * Default constructor
     * @param timBaseUrl TIM base URL, e.g. "https://trifork.com"
     * @param realm Realm, e.g. "my-test-realm"
     * @param clientId Client id, e.g. "my-client-id"
     * @param redirectUri Redirect URI, e.g. "my-app:/"
     * @param scopes Scopes, e.g. [[OIDScopeOpenID], [OIDScopeProfile]]
     * @param additionalParameters Additional parameters, e.g.
     * @param encryptionMethod Optional encryption method, [TIMESEncryptionMethod.AesGcm] is default and only supported
     * @param keyServiceVersion Optional key service version, defaults to [TIMKeyServiceVersion.V1]
     * @param backwardSupportConfiguration Optional configuration to support refresh tokens from old tokens.
     */
    constructor(timBaseUrl: URL,
                realm: String,
                clientId: String,
                redirectUri: Uri,
                scopes: List<String>,
                additionalParameters: Map<String, String> = mapOf(),
                encryptionMethod: TIMESEncryptionMethod = TIMESEncryptionMethod.AesGcm,
                keyServiceVersion: TIMKeyServiceVersion = TIMKeyServiceVersion.V1, prompts: List<String>? = null,
                backwardSupportConfiguration: TIMBackwardSupportConfiguration? = null) {

        val fullTimUrl = Uri.parse("${timBaseUrl}/auth/realms/$realm")

        this.oidcConfig = TIMOpenIdConnectConfiguration(
            issuerUri = fullTimUrl,
            clientId = clientId,
            redirectUri = redirectUri,
            scopes = scopes,
            additionalParameters = additionalParameters,
            prompts = prompts,
            backwardSupportConfiguration = backwardSupportConfiguration?.issuer?.let {
                return@let BackwardSupportConfiguration(issuer = Uri.parse("${it}/auth/realms/$realm"))
            }
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

data class TIMBackwardSupportConfiguration(val issuer: Uri)

