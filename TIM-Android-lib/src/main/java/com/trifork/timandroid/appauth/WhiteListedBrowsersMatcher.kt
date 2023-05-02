package com.trifork.timandroid.appauth

import android.util.Log
import com.trifork.timandroid.TIM
import net.openid.appauth.browser.BrowserDescriptor
import net.openid.appauth.browser.BrowserMatcher

private val whitelistedBrowsers = listOf(
    "com.android.chrome",
    "org.mozilla.firefox",
    "com.android.browser",
    "org.mozilla.focus",
    "com.duckduckgo.mobile.android",
    "com.microsoft.emmx"
)

class WhiteListedBrowsersMatcher : BrowserMatcher {

    override fun matches(b: BrowserDescriptor): Boolean {
        val match = b.useCustomTab && whitelistedBrowsers.contains(b.packageName)
        if (!match) {
            TIM.logger?.log(Log.ERROR, Companion.TAG, "Tried using unsupported browser: ${b.packageName}")
        }
        return match
    }

    companion object {
        private const val TAG = "WhiteListedBrowsersMatcher"
    }
}