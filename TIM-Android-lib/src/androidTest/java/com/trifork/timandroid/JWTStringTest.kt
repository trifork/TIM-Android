package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.helpers.*
import org.junit.Assert.*
import org.junit.Test

import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JWTStringTest {

    private val simpleAccessToken : JWTString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZXhwIjoxMzM3LCJpc3MiOiJodHRwczovL3RyaWZvcmsuY29tIn0.C_ERZ4zqvSLL0x1klZxERDzyDsjSFM-AYAsn0sdFmqE"
    private val invalidJwt : JWTString = "INVALID"

    @Test
    fun JWTString_userId() {
        assertEquals("user", simpleAccessToken.userId)
    }

    @Test
    fun JWTString_issuer() {
        assertEquals("https://trifork.com", simpleAccessToken.issuer)
    }

    @Test
    fun JWTString_expire() {
        assertEquals(1337, simpleAccessToken.expire)
    }

    @Test
    fun JWTString_Invalid() {
        assertNull(invalidJwt.userId)
    }

    @Test
    fun JWTString_init() {
        // Missing exp
        val jwtWithoutExp : JWTString = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        assertNotNull(jwtWithoutExp)

        // Missing sub
        val jwtWithoutUserId : JWTString = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6MTUxNjIzOTAyMn0.yOZC0rjfSopcpJ-d3BWE8-BkoLR_SCqPdJpq8Wn-1Mc"
        assertNotNull(jwtWithoutUserId)
    }

    @Test
    fun string_convert() {
        val byteArray = simpleAccessToken.convert
        val accessToken = convert(byteArray)
        assertEquals(accessToken, simpleAccessToken)
    }
}