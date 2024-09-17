package com.trifork.timandroid.helpers

import android.util.Base64
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import org.json.JSONObject

public object JWTDecoder {

    public fun decode(
        jwtToken: String
    ): TIMResult<Map<String, Any>, Throwable> {
        val segments: List<String> = jwtToken.split(".")
        //TODO validate cryptographic here
        return if (segments.size > 2) {
            decodeJWTPart(segments[1])
        } else {
            Throwable("Missing jwt segment(s), got ${segments.size} parts").toTIMFailure()
        }
    }

    private fun decodeJWTPart(
        value: String
    ): TIMResult<Map<String, Any>, Throwable> {
        return try {
            parseJWT(value).toTIMSuccess()
        } catch (throwable: Throwable) {
            throwable.toTIMFailure()
        }
    }

    @Throws(org.json.JSONException::class)
    private fun parseJWT(
        fromString: String
    ): HashMap<String, Any> {
        val bodyData = String(
            bytes = Base64.decode(fromString, Base64.URL_SAFE),
            charset = Charsets.UTF_8
        )
        val json = JSONObject(bodyData)
        return json.toMap()
    }

    @Throws(org.json.JSONException::class)
    private fun JSONObject.toMap(): HashMap<String, Any> {
        val list = HashMap<String, Any>()
        keys().forEach { it: String ->
            list[it] = get(it)
        }
        return list
    }

}