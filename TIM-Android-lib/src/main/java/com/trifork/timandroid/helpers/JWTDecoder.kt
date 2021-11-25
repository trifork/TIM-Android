package com.trifork.timandroid.helpers

import android.util.Base64
import org.json.JSONObject
import kotlin.math.ceil


object JWTDecoder {

    fun decode(jwtToken: String) : Map<String, Any> {
        val segments = jwtToken.split(".")
        return if(segments.size > 2) decodeJWTPart(segments.get(1)) else hashMapOf()
    }

    //TODO(Introduce moshi when implementing API)
    private fun decodeJWTPart(value: String) : Map<String, Any> {
        //Soround by catch
        val bodyData = base64UrlDecode(value)
        val json = JSONObject(String(bodyData))
        val list = HashMap<String, Any>()
        json.keys().forEach {
            list[it] = json.get(it)
        }
        return list
    }

    private fun base64UrlDecode(value: String) : ByteArray {
        var base64 = value
            .replace("-", "+")
            .replace("_", "/")

        val length = base64.toByteArray(Charsets.UTF_8).size.toDouble()
        val requiredLength = 4 * ceil(length / 4)
        val paddingLength = requiredLength - length

        if(paddingLength > 0) {
            val padding = "".padStart(paddingLength.toInt(), '=')
            base64 += padding
        }

        return Base64.decode(base64, Base64.URL_SAFE)
    }
}