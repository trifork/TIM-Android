package com.trifork.timandroid.helpers

public typealias JWTString = String
public typealias JWTDecodedMap = Map<String, Any>

private object JWTClaims {
    const val ISSUER_KEY = "iss"
    const val SUB_KEY = "sub"
    const val AUDIENCE_KEY = "aud"
    const val EXPIRE_KEY = "exp"
    const val NOT_BEFORE_KEY = "nbf"
    const val ISSUED_AT_KEY = "iat"

    const val NAME_KEY = "name"

    const val AZP_KEY = "azp"
}


public val JWTDecodedMap.expire: Int?
    get() = this[JWTClaims.EXPIRE_KEY] as? Int

public val JWTDecodedMap.userId: String?
    get() = this[JWTClaims.SUB_KEY] as? String


public val JWTDecodedMap.issuer: String?
    get() = this[JWTClaims.ISSUER_KEY] as? String

public val JWTDecodedMap.audience: String?
    get() = this[JWTClaims.AUDIENCE_KEY] as? String

public val JWTDecodedMap.notBefore: String?
    get() = this[JWTClaims.NOT_BEFORE_KEY] as? String


public val JWTDecodedMap.issuedAt: String?
    get() = this[JWTClaims.ISSUED_AT_KEY] as? String

public val JWTDecodedMap.name: String?
    get() = this[JWTClaims.NAME_KEY] as? String

public val JWTDecodedMap.authorizedParty: String?
    get() = this[JWTClaims.AZP_KEY] as? String
