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
        fun invalidCharsInClaimShouldStillParse() {
            val jwtResult = JWT.newInstance("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJxMjJJMVdlYkp2T09WTVNDblF5NlBqX1NqWFpLTENCajNfaVF2OXV3YWJNIn0.eyJleHAiOjE2ODM1MzMyODQsIm5iZiI6MCwiaWF0IjoxNjgzNTI5Njg0LCJhdXRoX3RpbWUiOjE2ODMyMDY2NzUsImp0aSI6ImFmZDMyNDdiLTJlZDUtNGMxNC05ZTU1LWNhYWY2MWQyZGI5NyIsImlzcyI6Imh0dHBzOi8vb2lkYy10ZXN0LnN1bmRoZWRzZGF0YXN0eXJlbHNlbi5kay9hdXRoL3JlYWxtcy9zZHMiLCJhdWQiOiJtbGFwcF9tb2NrIiwic3ViIjoiNDMwYzMxMjctZWQwNi00YTVlLWFmNTYtOTQxMTc5OWExN2FlIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoibWxhcHBfbW9jayIsIm5vbmNlIjoiS0VlVXZPUWM2cXpZVVhhWWw3aFlRZyIsInNlc3Npb25fc3RhdGUiOiJjOWVjNTA5My05ZGI0LTQ4YWQtOWQ3ZC1kNjU2YmM5MTBiOGYiLCJhY3IiOiIxIiwic2NvcGUiOiJvcGVuaWQgZGRzIGZvcmxvZWJzcGxhbmVyIG1pbmxvZyBwbHNwIGZtayBtaW5zcGFlcnJpbmcgb2ZmbGluZV9hY2Nlc3Mgc29zaS1zdHMgcHJvZmlsZSBldmVudGJveCBkZHYiLCJzaWQiOiJjOWVjNTA5My05ZGI0LTQ4YWQtOWQ3ZC1kNjU2YmM5MTBiOGYiLCJjcHIiOiIyNTEyNDg5OTk2IiwiY2VydHN1YmRuIjoiQ049TGlzIFPvv71yZW5zZW4rc2VyaWFsbnVtYmVyPVBJRDpwaWQwMzAxMDEwMDAzLE89SW5nZW4gb3JnYW5pc2F0b3Jpc2sgdGlsa255dG5pbmcsQz1ESyIsIm5hbWUiOiJMaXMgU--_vXJlbnNlbiIsInByZWZlcnJlZF91c2VybmFtZSI6InBpZC0yNTEyNDg5OTk2IiwiZ2l2ZW5fbmFtZSI6IkxpcyIsImZhbWlseV9uYW1lIjoiU--_vXJlbnNlbiJ9.mrrY0XMTI5ZRjECj3sZJ8T5-nQe_-hjtZtIR8DqLxbAl9O3ZhLvICJ1CACObw_KVNyVPe5mrqjDMWvs95orSKSSmI1UzHPUr8of8ZAD-lp6hGQce90KIPO2gMIgan4Cq2GS7auEnilKVBg_XNMKS2yaCpW0F8T1igq1fSvafaimnc91fJt1mDYF2M2M0B2TWgsFA4MuLH1dKhd9oBUkK1QR0nwAokeD2rm6DFufPIATSOS7RMlQz2WOuO2Ks3xl6SAyj8SWOqoI0C-woXuDFx6CkJU9I7yMEjdTEjpvBThnuSLc_biM4zaB3mI8MBpwWt8yXaJKSGDvh05mkGD7N-w")
            jwtResult.assertIs<TIMResult.Success<JWT>>()
            jwtResult.value.userId.assert("430c3127-ed06-4a5e-af56-9411799a17ae")
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