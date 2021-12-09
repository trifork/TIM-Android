package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.helpers.ext.convertToByteArray
import com.trifork.timandroid.helpers.ext.convertToString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StringTests {

    private val string  = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

    @Test
    fun string_convert_bytearray_convert_is_converted() {
        val byteArray = string.convertToByteArray()
        val accessToken = byteArray.convertToString()
        Assert.assertEquals(accessToken, string)
    }


    private val token = "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJiMGFmMDRiNy1iZjFiLTRhOTMtODMxMi0wNDk2MTBlZjU1MjAifQ.eyJleHAiOjAsIm5iZiI6MCwiaWF0IjoxNjM4NDQ0ODM1LCJqdGkiOiJhZTY3NDBmNy0zNTdmLTQ1NWEtYjVhYi04OWE1YWJiYTg3OTgiLCJpc3MiOiJodHRwczovL29pZGMtdGVzdC5ob3N0ZWQudHJpZm9yay5jb20vYXV0aC9yZWFsbXMvZGV2IiwiYXVkIjoiaHR0cHM6Ly9vaWRjLXRlc3QuaG9zdGVkLnRyaWZvcmsuY29tL2F1dGgvcmVhbG1zL2RldiIsInN1YiI6IjczMTBlNmM5LTk5ODctNDNhYy1iOWFiLWM5MTI0NmNiNTY2ZCIsInR5cCI6Ik9mZmxpbmUiLCJhenAiOiJ0ZXN0X21vY2siLCJub25jZSI6ImduRE1MRWRRQVNYWnZsY3B6UlZ1N3ciLCJzZXNzaW9uX3N0YXRlIjoiMDA3YmUwM2EtMmFjMi00MmY1LTljMjktZGQwZWNmZGJkMTViIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBvZmZsaW5lX2FjY2VzcyBlbWFpbCIsInNpZCI6IjAwN2JlMDNhLTJhYzItNDJmNS05YzI5LWRkMGVjZmRiZDE1YiJ9.jbSLsZ-zHMXyjHim5j8R5wtb7KrZ9UFxMB5mFl4fAa0"

    @Test
    fun string_convert() {
        val byteArray = token.convertToByteArray()
        val convertedToken = byteArray.convertToString()
        Assert.assertEquals(convertedToken, token)
    }
}