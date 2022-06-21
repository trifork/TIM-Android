package com.trifork.timandroid.models.errors

import android.net.*
import com.trifork.timandroid.models.*
import com.trifork.timandroid.models.openid.*
import com.trifork.timandroid.testHelpers.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TIMConfigurationTests {
    @Test
    fun oidcConfigShouldBeSetupCorrectly() {
        val config = TIMConfiguration(
            timBaseUrl = URL("https://trifork.com"),
            realm = "my-test-realm",
            clientId = "clientId",
            redirectUri = Uri.parse("my-app://:/"),
            scopes = listOf(OIDScopeOpenID, OIDScopeProfile)
        )

        config.oidcConfig.issuerUri.toString()
            .assert("https://trifork.com/auth/realms/my-test-realm")
    }

}