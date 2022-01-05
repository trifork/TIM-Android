package com.trifork.timandroid.helpers

class BiometricRefreshToken(
    val refreshToken: JWT,
    val longSecret: String
)

//TODO(Add cast of expireDate to actual DateTime format)
class JWT(
    val token: JWTString,
    val userId: String,
    val expireDate: Int?,
    val issuer: String?
) {
    companion object {
        fun newInstance(token: JWTString): JWT? {
            val userId = token.userId ?: return null
            return JWT(
                token,
                userId,
                token.expire,
                token.issuer
            )
        }
    }
}

