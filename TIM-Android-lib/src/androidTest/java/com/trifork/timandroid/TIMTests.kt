package com.trifork.timandroid

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trifork.timandroid.helpers.TIMLoggerInternal
import com.trifork.timandroid.models.TIMConfiguration
import com.trifork.timandroid.models.openid.OIDScopeOpenID
import com.trifork.timandroid.models.openid.OIDScopeProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class TIMTests {

    val config = TIMConfiguration(
        URL("https://trifork.com"),
        "my-test-realm",
        "clientId",
        Uri.parse("my-app://:/"),
        listOf(OIDScopeOpenID, OIDScopeProfile)
    )

    @Test
    fun testConfigure() {
        assertFalse(TIM.isConfigured)
        TIM.configure(config, TIMLoggerInternal(), InstrumentationRegistry.getInstrumentation().context)
        assertTrue(TIM.isConfigured)
    }

    @Test
    fun testReconfigure() {
        assertTrue(TIM.isConfigured)
        TIM.configure(config, TIMLoggerInternal(), InstrumentationRegistry.getInstrumentation().context, true)
        assertTrue(TIM.isConfigured)
        TIM.configure(config, TIMLoggerInternal(), InstrumentationRegistry.getInstrumentation().context, true)
        assertTrue(TIM.isConfigured)
    }


}