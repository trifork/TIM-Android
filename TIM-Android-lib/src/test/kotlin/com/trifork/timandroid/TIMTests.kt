package com.trifork.timandroid

import android.net.*
import com.trifork.timandroid.models.*
import com.trifork.timandroid.models.openid.*
import com.trifork.timandroid.testHelpers.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.*
import java.net.*

@RunWith(RobolectricTestRunner::class)
class TIMTests {

    @RunWith(RobolectricTestRunner::class)
    class Configure {
        @Test
        fun shouldPopulateFields() {
            val tim = TIMImpl()
            tim.configure(config, context = RuntimeEnvironment.getApplication())
            tim.storage.assertNotNull()
            tim.auth.assertNotNull()
            tim.logger.assertNotNull()
        }

        @Test
        fun allowsReconfigureIfTrue() {
            val tim = TIMImpl()
            tim.configure(config, context = RuntimeEnvironment.getApplication())
            tim.configure(
                config = config,
                context = RuntimeEnvironment.getApplication(),
                allowReconfigure = true
            )
        }

        @Test
        fun throwsOnReconfigureIfSetToFalse() {
            val tim = TIMImpl()
            tim.configure(config, context = RuntimeEnvironment.getApplication())
            Assert.assertThrows(Exception::class.java) {
                tim.configure(
                    config = config,
                    context = RuntimeEnvironment.getApplication(),
                    allowReconfigure = false
                )
            }
        }
    }

    @Test
    fun isConfigured() {
        val tim = TIMImpl()
        tim.isConfigured.assertFalse()
        tim.configure(config, context = RuntimeEnvironment.getApplication())
        tim.isConfigured.assertTrue()
    }

    companion object {

        val config = TIMConfiguration(
            URL("https://trifork.com"),
            "my-test-realm",
            "clientId",
            Uri.parse("my-app://:/"),
            listOf(OIDScopeOpenID, OIDScopeProfile)
        )
    }
}