package com.dandi.nyummy.common.data

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response

abstract class BaseRemoteDataSource {
    protected inline fun <reified T> checkResponse(response: Response<T>): T {
        if (response.isSuccessful) {
            response.body()?.let { return it }
            // 204 No Content 처럼 바디 없는 성공 응답은 Retrofit 이 body 를 null 로 반환한다.
            // 호출부가 Unit 응답을 기대하는 경우에만 정상 처리한다.
            if (Unit is T) return Unit
            throw IllegalStateException("Successful response with null body: ${response.raw().request.url}")
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
            // domain 은 cause.message 를 서버 code 로 읽는다(serverErrorCode).
            // 공통 에러 바디 {"code":"api...","message":"..."} 의 code 만 담고, code 가 없으면 비워 둔다.
            // 원문 바디는 디버그용으로 msg 에만 남긴다.
            cause = extractErrorCode(errorBody)?.let(::Throwable),
        )
    }

    private fun extractErrorCode(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return runCatching {
            Json.parseToJsonElement(errorBody).jsonObject["code"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }
}
