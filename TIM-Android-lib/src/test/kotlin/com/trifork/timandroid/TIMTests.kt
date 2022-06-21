package com.trifork.timandroid

import android.net.*
import com.trifork.timandroid.models.*
import com.trifork.timandroid.models.openid.*
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
            Assert.assertNotNull(tim.storage)
            Assert.assertNotNull(tim.auth)
            Assert.assertNotNull(tim.logger)
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
        Assert.assertFalse(tim.isConfigured)
        tim.configure(config, context = RuntimeEnvironment.getApplication())
        Assert.assertTrue(tim.isConfigured)
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