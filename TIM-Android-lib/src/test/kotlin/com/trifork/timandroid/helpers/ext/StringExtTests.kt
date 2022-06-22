package com.trifork.timandroid.helpers.ext

import com.trifork.timandroid.testHelpers.*
import org.junit.Test

class StringExtTests {

    private val testAsByteArray = byteArrayOf(116, 101, 115, 116) //the text "test"

    @Test
    fun byteArrayConvertToString() {
        val converted = testAsByteArray.convertToString()
        converted.assert("test")
    }

    @Test
    fun stringConvertToByteArray() {
        val exampleString = "test"
        val converted = exampleString.convertToByteArray()
        converted.assert(testAsByteArray)
    }
}