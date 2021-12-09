package com.trifork.timandroid

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.models.TIMConfiguration
import com.trifork.timandroid.models.openid.OIDScopeOpenID
import com.trifork.timandroid.models.openid.OIDScopeProfile
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class TIMConfigurationTests {

    @Test
    fun testDefaultConstructor() {
        val config = TIMConfiguration(
            URL("https://trifork.com"),
            "my-test-realm",
            "clientId",
            Uri.parse("my-app://:/"),
            listOf(OIDScopeOpenID, OIDScopeProfile)
        )

        Assert.assertEquals(config.oidcConfig.issuerUri.toString(), "https://trifork.com/auth/realms/my-test-realm")
    }

}