package com.dandi.nyummy.common.data

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

abstract class BaseRemoteDataSource {
    protected fun <T> checkResponse(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body()
                ?: throw IllegalStateException("Successful response with null body: ${response.raw().request.url}")
        }
        throw response.toHttpResponseException()
    }

    protected inline fun <T, R> checkResponse(
        response: Response<T>,
        crossinline returnValue: (T) -> R,
    ): R {
        if (response.isSuccessful) {
            val body = response.body()
                ?: throw IllegalStateException("Successful response with null body: ${response.raw().request.url}")
            return returnValue(body)
        }
        throw response.toHttpResponseException()
    }

    protected fun <T> Response<T>.toHttpResponseException(): HttpResponseException {
        val errorBody = errorBody()?.string()
        return HttpResponseException(
            status = HttpResponseStatus.create(code()),
            rawCode = code(),
            errorRequestUrl = raw().request.url.toString(),
            msg = "Http Request Failed (${code()}) ${message()}, $errorBody",
            // handlingErrorOnUseCase 가 ErrorType.type 과 cause.message 를 비교하므로,
            // 공통 에러 바디 {"code":"api...","message":"..."} 의 code 만 추출해 담는다.
            cause = (extractErrorCode(errorBody) ?: errorBody)?.let(::Throwable),
        )
    }

    private fun extractErrorCode(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return runCatching {
            Json.parseToJsonElement(errorBody).jsonObject["code"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}
