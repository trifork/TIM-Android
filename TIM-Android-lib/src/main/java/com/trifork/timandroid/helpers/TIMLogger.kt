package com.trifork.timandroid.helpers

import android.util.Log
import com.trifork.timandroid.TIM
import com.trifork.timencryptedstorage.helpers.TIMEncryptedStorageLogger

interface TIMLogger {
    fun log(priority: Int, tag: String, msg: String, throwable: Throwable? = null)
}

class TIMLoggerInternal : TIMLogger {
    override fun log(priority: Int, tag: String, msg: String, throwable: Throwable?) {
        when (priority) {
            Log.VERBOSE -> Log.v(tag, msg, throwable)
            Log.DEBUG -> Log.d(tag, msg, throwable)
            Log.INFO -> Log.i(tag, msg, throwable)
            Log.WARN -> Log.w(tag, msg, throwable)
            Log.ERROR -> Log.e(tag, msg, throwable)
        }
    }
}

class TIMEncryptedStorageLoggerInternal : TIMEncryptedStorageLogger {
    override fun log(priority: Int, tag: String, msg: String, throwable: Throwable?) {
        TIM.logger?.log(priority, tag, msg, throwable)
    }
}