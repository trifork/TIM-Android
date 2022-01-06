package com.trifork.timandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trifork.timandroid.helpers.JWT
import com.trifork.timandroid.helpers.JWTString
import com.trifork.timencryptedstorage.models.TIMResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JWTStringTests {

    private val simpleAccessToken : JWTString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZXhwIjoxMzM3LCJpc3MiOiJodHRwczovL3RyaWZvcmsuY29tIn0.C_ERZ4zqvSLL0x1klZxERDzyDsjSFM-AYAsn0sdFmqE"
    private val invalidJwt : JWTString = "INVALID"

    @Test
    fun JWT_newInstance() {
        val jwtResult = JWT.newInstance(simpleAccessToken) as TIMResult.Success
        val jwt = jwtResult.value

        assertEquals("user", jwt.userId)
        assertEquals("https://trifork.com", jwt.issuer)
        assertEquals("1970-01-01T00:22:17Z", jwt.expire)
    }

    @Test
    fun JWTString_Invalid() {
        val jwtResult = JWT.newInstance(invalidJwt)

        assertEquals(TIMResult.Failure::class, jwtResult::class)
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
}