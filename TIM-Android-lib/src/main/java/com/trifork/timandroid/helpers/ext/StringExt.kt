package com.trifork.timandroid.helpers.ext

//TODO Which encoding should we use?
private val CHARSET = Charsets.UTF_8

fun ByteArray.convertToString() = String(this, CHARSET)

fun String.convertToByteArray() = this.toByteArray(CHARSET)