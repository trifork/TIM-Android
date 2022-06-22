package com.trifork.timandroid.helpers

import com.trifork.timandroid.testHelpers.*
import com.trifork.timencryptedstorage.models.*
import com.trifork.timencryptedstorage.models.errors.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.*

class JWTTests {

    @RunWith(RobolectricTestRunner::class)
    class NewInstance {

        @Test
        fun validAccessTokenShouldBeParsed() {
            val jwtResult = JWT.newInstance(simpleAccessToken)
            jwtResult.assertIs<TIMResult.Success<JWT>>()
            val jwt = jwtResult.value
            jwt.userId.assert("user")
            jwt.issuer.assert("https://trifork.com")
            jwt.expire.assert("1970-01-01T00:22:17Z")
        }

        @Test
        fun invalidJwtShouldFail() {
            val jwtResult = JWT.newInstance(invalidJwt)
            jwtResult.assertIs<TIMResult.Failure<TIMEncryptedStorageError.KeyServiceJWTDecodeFailed>>()
        }

        @Test
        fun shouldHandleMissingExpiration() {
            val jwtResult = JWT.newInstance(missingExp)
            jwtResult.assertIs<TIMResult.Success<JWT>>()
            jwtResult.value.expire.assertNull()
            jwtResult.value.userId.assert("1234567890")
        }

        @Test
        fun shouldFailOnMissingSubject() {
            val jwtResult = JWT.newInstance(missingSub)
            jwtResult.assertIs<TIMResult.Failure<TIMEncryptedStorageError.KeyServiceJWTDecodeFailed>>()
            jwtResult.error.error.message.assert(JWT.MissingUserIdException.message!!)
        }
    }

    companion object {
        const val simpleAccessToken: JWTString =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZXhwIjoxMzM3LCJpc3MiOiJodHRwczovL3RyaWZvcmsuY29tIn0.C_ERZ4zqvSLL0x1klZxERDzyDsjSFM-AYAsn0sdFmqE"
        const val invalidJwt: JWTString = "INVALID"

        const val missingExp: JWTString =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

        const val missingSub: JWTString =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjIsImV4cCI6MTUxNjIzOTAyMn0.yOZC0rjfSopcpJ-d3BWE8-BkoLR_SCqPdJpq8Wn-1Mc"
    }
}