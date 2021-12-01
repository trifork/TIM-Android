package com.trifork.timandroid.helpers.ext

private val CHARSET = Charsets.UTF_8

fun ByteArray.convertToString() = this.toString(CHARSET)

fun String.convertToByteArray() = this.toByteArray(CHARSET)