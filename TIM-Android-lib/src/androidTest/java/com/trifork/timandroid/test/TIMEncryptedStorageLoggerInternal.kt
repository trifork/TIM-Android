package com.trifork.timencryptedstorage.test

import android.util.Log
import com.trifork.timencryptedstorage.helpers.TIMEncryptedStorageLogger

class TIMEncryptedStorageLoggerInternal : TIMEncryptedStorageLogger {
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