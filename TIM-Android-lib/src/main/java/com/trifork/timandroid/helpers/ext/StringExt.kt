package com.trifork.timandroid.helpers.ext

internal fun ByteArray.convertToString(

): String = String(this, Charsets.UTF_8)

internal fun String.convertToByteArray(

): ByteArray = this.toByteArray(Charsets.UTF_8)