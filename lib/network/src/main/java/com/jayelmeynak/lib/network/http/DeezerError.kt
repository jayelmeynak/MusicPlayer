package com.jayelmeynak.lib.network.http

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jayelmeynak.util.result.DataError
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

// Deezer reports errors with HTTP 200 and a top-level {"error":{"type","message","code"}} body.
internal class DeezerApiException(val code: Int?) : IOException("Deezer error, code $code")

internal fun deezerErrorToRemote(code: Int?): DataError.Remote = when (code) {
    QUOTA_EXCEEDED -> DataError.Remote.TOO_MANY_REQUESTS
    SERVICE_BUSY -> DataError.Remote.SERVER
    DATA_NOT_FOUND -> DataError.Remote.NOT_FOUND
    else -> DataError.Remote.UNKNOWN
}

// Must be added before the Gson factory: it reads the body, throws on a Deezer error
// and hands the same text to the next converter otherwise.
internal object DeezerErrorConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter<ResponseBody, Any> { body ->
            val contentType = body.contentType()
            val text = body.use { it.string() }
            deezerErrorOrNull(text)?.let { throw it }
            delegate.convert(text.toResponseBody(contentType))
        }
    }

    private fun deezerErrorOrNull(text: String): DeezerApiException? {
        val root = runCatching { JsonParser.parseString(text) }.getOrNull()
        val error = (root as? JsonObject)?.get("error") as? JsonObject ?: return null
        val code = error.get("code")
            ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            ?.asInt
        return DeezerApiException(code)
    }
}

private const val QUOTA_EXCEEDED = 4
private const val SERVICE_BUSY = 700
private const val DATA_NOT_FOUND = 800
