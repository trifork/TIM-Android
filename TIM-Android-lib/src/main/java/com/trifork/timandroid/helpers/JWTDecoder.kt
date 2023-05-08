package com.trifork.timandroid.helpers

import android.util.Base64
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import org.json.JSONObject

object JWTDecoder {

    fun decode(jwtToken: String): TIMResult<Map<String, Any>, Throwable> {
        val segments = jwtToken.split(".")
        return if (segments.size > 2) {
            decodeJWTPart(segments[1])
        } else {
            Throwable("Missing jwt segment 1").toTIMFailure()
        }
    }

    private fun decodeJWTPart(value: String): TIMResult<Map<String, Any>, Throwable> {
        return try {
            parseJWT(value).toTIMSuccess()
        } catch (throwable: Throwable) {
            throwable.toTIMFailure()
        }
    }

    @Throws
    private fun parseJWT(fromString: String): HashMap<String, Any> {
        val bodyData = String(Base64.decode(fromString, Base64.URL_SAFE), Charsets.UTF_8)
        val json = JSONObject(bodyData)
        return json.toMap()
    }

    private fun JSONObject.toMap(): HashMap<String, Any> {
        val list = HashMap<String, Any>()
        keys().forEach {
            list[it] = get(it)
        }
        return list
    }

}