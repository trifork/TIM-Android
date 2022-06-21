package com.trifork.timandroid.helpers

import org.junit.*


class JWTStringTests {

    @Test
    fun jwtDecodedMapExpire() {
        val map = mutableMapOf<String, Any>()
        Assert.assertNull(map.expire)
        map["exp"] = 1234
        Assert.assertEquals(1234, map.expire)
    }

    @Test
    fun jwtDecodedMapUserId() {
        val map = mutableMapOf<String, Any>()
        Assert.assertNull(map.userId)
        map["sub"] = "1234"
        Assert.assertEquals("1234", map.userId)
    }
}