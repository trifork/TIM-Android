package com.trifork.timandroid.helpers

typealias JWTString = String

private const val EXPIRE_KEY = "exp"
private const val SUB_KEY = "sub"
private const val ISSUER_KEY = "iss"

//TODO(change cast to some DateTime format)
val JWTString.expire: Int?
    get() = JWTDecoder.decode(this)[EXPIRE_KEY] as? Int

val JWTString.userId: String?
    get() = JWTDecoder.decode(this)[SUB_KEY] as? String

val JWTString.issuer: String?
    get() = JWTDecoder.decode(this)[ISSUER_KEY] as? String

private val CHARSET = Charsets.UTF_8

val String.convert: ByteArray
    get() = this.toByteArray(CHARSET)

//TODO(Do we want this to be on a companion object so we can have it linked to String.convert?)
fun convert(byteArray: ByteArray) : String = byteArray.toString(CHARSET)
