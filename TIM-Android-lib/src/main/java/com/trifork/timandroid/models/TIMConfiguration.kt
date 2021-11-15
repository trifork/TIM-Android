package com.trifork.timandroid.models

import com.trifork.timandroid.models.openid.TIMOpenIdConnectConfiguration
import com.trifork.timencryptedstorage.keyservice.TIMKeyService
import com.trifork.timencryptedstorage.models.TIMESEncryptionMethod
import com.trifork.timencryptedstorage.models.keyservice.TIMKeyServiceConfiguration
import com.trifork.timencryptedstorage.securestorage.TIMSecureStorage


/**
 * Combined configuration for AppAuth and TIMEncryptedStorage
 * @param oidcConfig The OIDC configuration
 * @param keyServiceConfig The [TIMKeyService]
 * @param encryptionMethod The encryption method used by TIM (under the hood it's used by [TIMSecureStorage]). Recommended method is [TIMESEncryptionMethod.AesGcm]
 */

class TIMConfiguration(
    val oidcConfig: TIMOpenIdConnectConfiguration,
    val keyServiceConfig: TIMKeyServiceConfiguration,
    val encryptionMethod: TIMESEncryptionMethod = TIMESEncryptionMethod.AesGcm
) {


}

