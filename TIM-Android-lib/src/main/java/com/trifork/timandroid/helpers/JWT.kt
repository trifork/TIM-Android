package com.trifork.timandroid.helpers

typealias JWT = String

private const val EXPIRE_KEY = "exp"
private const val SUB_KEY = "sub"
private const val ISSUER_KEY = "iss"

//TODO(add cast to some DateTime format)
val JWT.expire: Int?
    get() = JWTDecoder.decode(this)[EXPIRE_KEY] as? Int

val JWT.userId: String?
    get() = JWTDecoder.decode(this)[SUB_KEY] as? String

val JWT.issuer: String?
    get() = JWTDecoder.decode(this)[ISSUER_KEY] as? String