package com.trifork.timandroid.helpers

import android.util.Base64
import com.trifork.timandroid.helpers.JWTDecoder.padForBase64
import com.trifork.timandroid.helpers.ext.*
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.toTIMFailure
import com.trifork.timencryptedstorage.models.toTIMSuccess
import org.json.JSONObject
import kotlin.math.ceil

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
        val bodyData = base64UrlDecode(fromString)
        val json = JSONObject(String(bodyData))
        return json.toMap()
    }

    private fun JSONObject.toMap(): HashMap<String, Any> {
        val list = HashMap<String, Any>()
        keys().forEach {
            list[it] = get(it)
        }
        return list
    }

    private fun base64UrlDecode(value: String): ByteArray {
        val base64 = value.replaceUrlBase64Chars()
        val paddingLength = base64.getBase64PaddingLength()
        val paddedBase64 = base64.padIfNeededForBase64(paddingLength)
        return Base64.decode(paddedBase64, Base64.URL_SAFE)
    }

    private fun String.replaceUrlBase64Chars(): String {
        return replace("-", "+")
            .replace("_", "/")
    }

    private fun String.getBase64PaddingLength(): Int {
        val length = convertToByteArray().size.toDouble()
        val requiredLength = 4 * ceil(length / 4)
        return (requiredLength - length).toInt()
    }

    private fun String.padIfNeededForBase64(paddingLength: Int): String {
        return if (paddingLength > 0) {
            this.padForBase64(paddingLength)
        } else {
            this
        }
    }

    private fun String.padForBase64(paddingLength: Int): String {
        val padding = "".padStart(paddingLength, '=')
        return this + padding
    }
}