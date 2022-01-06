package com.trifork.timandroid.helpers

typealias JWTString = String
typealias JWTDecodedMap = Map<String, Any>

private const val EXPIRE_KEY = "exp"
private const val SUB_KEY = "sub"
private const val ISSUER_KEY = "iss"

val JWTDecodedMap.expire: Int?
    get() = this[EXPIRE_KEY] as? Int

val JWTDecodedMap.userId: String?
    get() = this[SUB_KEY] as? String

val JWTDecodedMap.issuer: String?
    get() = this[ISSUER_KEY] as? String