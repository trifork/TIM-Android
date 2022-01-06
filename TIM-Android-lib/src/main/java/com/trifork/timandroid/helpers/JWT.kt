package com.trifork.timandroid.helpers

import android.util.Log
import com.trifork.timandroid.TIM
import com.trifork.timandroid.internal.TIMDataStorageInternal
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.errors.TIMEncryptedStorageError
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BiometricRefreshToken(
    val refreshToken: JWT,
    val longSecret: String
)

class JWT(
    val token: JWTString,
    val userId: String,
    val expire: String?,
    val issuer: String?
) {
    companion object {
        fun newInstance(token: JWTString): TIMResult<JWT, TIMEncryptedStorageError.KeyServiceJWTDecodeFailed> {
            val jwtResult = JWTDecoder.decode(token)
            val jwt = when (jwtResult) {
                is TIMResult.Failure -> return TIMEncryptedStorageError.KeyServiceJWTDecodeFailed(jwtResult.error).toTIMFailure()
                is TIMResult.Success -> jwtResult.value
            }

            TIM.logger?.log(Log.DEBUG, TIMDataStorageInternal.TAG, "Decoded jwt token")

            val userId = jwt.userId ?: return TIMEncryptedStorageError.KeyServiceJWTDecodeFailed(Throwable("No userId in jwt")).toTIMFailure()
            val expire = jwt.expire?.toLong()

            var zonedDateTime : String? = null
            if(expire != null) {
                try {
                    zonedDateTime = Instant.ofEpochSecond(expire)
                        .atZone(ZoneId.of("Z"))
                        .format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                }
                catch (throwable: Throwable) {
                    TIM.logger?.log(Log.ERROR, TIMDataStorageInternal.TAG, "Could not decode JWT expire date time")
                }
            }

            return JWT(
                token,
                userId,
                zonedDateTime,
                jwt.issuer
            ).toTIMSuccess()
        }
    }
}

