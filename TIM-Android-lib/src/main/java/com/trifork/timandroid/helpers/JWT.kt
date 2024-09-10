@file:Suppress("unused")

package com.trifork.timandroid.helpers

import android.util.*
import com.trifork.timandroid.*
import com.trifork.timandroid.internal.*
import com.trifork.timencryptedstorage.models.*
import com.trifork.timencryptedstorage.models.errors.*
import java.time.*
import java.time.format.*

public data class BiometricRefreshToken(
    public val refreshToken: JWT,
    public val longSecret: String
)

public data class JWT(
    public val token: JWTString,
    public val userId: String,
    public val expire: String?,
    public val issuer: String?
) {
    public companion object {
        public fun newInstance(
            token: JWTString
        ): TIMResult<JWT, TIMEncryptedStorageError.KeyServiceJWTDecodeFailed> {
            val jwtResult: TIMResult<Map<String, Any>, Throwable> = JWTDecoder.decode(token)
            val jwt: Map<String, Any> = when (jwtResult) {
                is TIMResult.Failure -> return TIMEncryptedStorageError.KeyServiceJWTDecodeFailed(
                    jwtResult.error
                ).toTIMFailure()
                is TIMResult.Success -> jwtResult.value
            }

            TIM.logger?.log(
                priority = Log.DEBUG,
                tag = TIMDataStorageInternal.TAG,
                msg = "Decoded jwt token"
            )

            val userId: String = jwt.userId ?: return TIMEncryptedStorageError.KeyServiceJWTDecodeFailed(
                MissingUserIdException
            ).toTIMFailure()
            val expire: Long? = jwt.expire?.toLong()

            val zonedDateTime: String? = expire?.let { parseZonedDateTimeOrLog(it) }

            return JWT(
                token = token,
                userId = userId,
                expire = zonedDateTime,
                issuer = jwt.issuer
            ).toTIMSuccess()
        }

        private fun parseZonedDateTimeOrLog(expire: Long): String? {
            return try {
                Instant.ofEpochSecond(expire)
                    .atZone(ZoneId.of("Z"))
                    .format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
            } catch (throwable: Throwable) {
                TIM.logger?.log(
                    Log.ERROR,
                    TIMDataStorageInternal.TAG,
                    "Could not decode JWT expire date time"
                )
                null
            }
        }



        public val MissingUserIdException: Throwable = Throwable("No userId in jwt")

    }

}

