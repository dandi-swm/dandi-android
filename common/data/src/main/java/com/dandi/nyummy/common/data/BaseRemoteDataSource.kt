package com.dandi.nyummy.common.data

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import retrofit2.Response

abstract class BaseRemoteDataSource {
    protected fun <T> checkResponse(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body()
                ?: throw IllegalStateException("Successful response with null body: ${response.raw().request.url}")
        }
        val errorBody = response.errorBody()?.string()
        throw HttpResponseException(
            status = HttpResponseStatus.create(response.code()),
            rawCode = response.code(),
            errorRequestUrl = response.raw().request.url.toString(),
            msg = "Http Request Failed (${response.code()}) ${response.message()}, $errorBody",
            cause = errorBody?.let(::Throwable),
        )
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
        val errorBody = response.errorBody()?.string()
        throw HttpResponseException(
            status = HttpResponseStatus.create(response.code()),
            rawCode = response.code(),
            errorRequestUrl = response.raw().request.url.toString(),
            msg = "Http Request Failed (${response.code()}) ${response.message()}, $errorBody",
            cause = errorBody?.let(::Throwable),
        )
    }
}
