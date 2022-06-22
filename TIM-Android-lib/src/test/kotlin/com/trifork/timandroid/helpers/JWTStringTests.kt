package com.trifork.timandroid.helpers

import com.trifork.timandroid.testHelpers.*
import org.junit.*


class JWTStringTests {

    @Test
    fun jwtDecodedMapExpire() {
        val map = mutableMapOf<String, Any>()
        map.expire.assertNull()
        map["exp"] = 1234
        map.expire.assert(1234)
    }

    @Test
    fun jwtDecodedMapUserId() {
        val map = mutableMapOf<String, Any>()
        map.userId.assertNull()
        map["sub"] = "1234"
        map.userId.assert("1234")
    }
}