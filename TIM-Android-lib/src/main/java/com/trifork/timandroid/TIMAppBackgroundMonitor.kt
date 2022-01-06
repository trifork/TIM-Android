package com.trifork.timandroid

/**
 * Registers how long the app has been in the background, and invokes a timeout event when the app becomes active if the background time has exceeded the timeout value.
 */
interface TIMAppBackgroundMonitor {

    /**
     * Enabled detection of the duration the app has been in the background.
     * @param durationSeconds The duration (seconds) the app may be in the background. When this value is exceeded it will invoke "handleTimeout"
     * @param handleTimeout The function that is invoked when the timeout occurs
     */
    fun enable(durationSeconds: Long, handleTimeout: () -> Unit)

    /**
     *  Disables the background duration detection.
     */
    fun disable()

}