package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.helpers.ext.convertToByteArray
import com.trifork.timandroid.helpers.ext.convertToString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StringTest {

    private val string  = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

    @Test
    fun string_convert_bytearray_convert_is_converted() {
        val byteArray = string.convertToByteArray()
        val accessToken = byteArray.convertToString()
        Assert.assertEquals(accessToken, string)
    }
}