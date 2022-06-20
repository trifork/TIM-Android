@file:Suppress("unused")

package com.trifork.timandroid.internal

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.trifork.timandroid.TIMAppBackgroundMonitor
import java.time.ZonedDateTime

class TIMAppBackgroundMonitorInternal : TIMAppBackgroundMonitor, ApplicationLifecycleObserver() {

    companion object {
        fun newInstance(): TIMAppBackgroundMonitorInternal {
            return TIMAppBackgroundMonitorInternal()
        }
    }

    private var handleTimeout: (() -> Unit)? = null
    private var durationSeconds: Long = 18000 // 5 minutes
    private var wentToBackground: ZonedDateTime? = null

    //region TIMAppBackgroundMonitor
    override fun enable(durationSeconds: Long, handleTimeout: () -> Unit) {
        this.durationSeconds = durationSeconds
        this.handleTimeout = handleTimeout

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun disable() {
        removeObserver()
    }
    //endregion

    //region ApplicationLifecycleObserver
    override fun onAppForeground() {
        //Check duration since went to background
        val backgroundZonedDateTime = wentToBackground
        val handleTimeoutFunction = handleTimeout
        if (handleTimeoutFunction != null && backgroundZonedDateTime != null && ZonedDateTime.now().isAfter(backgroundZonedDateTime.plusSeconds(durationSeconds))) {
            handleTimeoutFunction()
        }
    }

    override fun onAppBackground() {
        wentToBackground = ZonedDateTime.now()
    }

    override fun onAppDestroy() {
        removeObserver()
    }
    //endregion

    //region Private helpers
    private fun removeObserver() {
        this.handleTimeout = null
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
    //endregion
}

abstract class ApplicationLifecycleObserver : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        onAppForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        onAppBackground()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onAppDestroy()
    }

    abstract fun onAppForeground()

    abstract fun onAppBackground()

    abstract fun onAppDestroy()
}