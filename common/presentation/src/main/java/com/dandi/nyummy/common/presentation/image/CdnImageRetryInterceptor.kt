package com.dandi.nyummy.common.presentation.image

import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import java.io.IOException

/**
 * 냐미 CDN 그림이 일시적인 이유(시간 초과, 연결 끊김, 5xx)로 실패하면 한 번 더 받는다.
 * 느린 망에서 큰 그림 여러 장이 한 연결을 나눠 쓰다 시간 초과로 실패하고 빈 칸으로 남던 문제를 줄인다.
 *
 * [CdnWebpFallbackInterceptor]보다 바깥에 둔다. 그래서 다시 받기는 `.webp → .png` 폴백까지 통째로 한 번 더 도는데,
 * 첫 시도에서 `.webp` 403/404를 이미 기억했으면 두 번째는 `.png`만 받는다.
 * 캐시만 찾는 요청(오프라인 폴백 등)과 CDN 밖 주소(식사 사진, 배너 등)는 건드리지 않는다.
 */
internal class CdnImageRetryInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data as? String
        if (url == null || !NyummyCdn.isCdnUrl(url) || !request.networkCachePolicy.readEnabled) return chain.proceed()
        return retryOnce(
            attempt = { chain.proceed() },
            shouldRetry = { it is ErrorResult && isRetryableImageFailure(it.throwable) },
        )
    }
}

/** [attempt]를 부르고, 결과가 [shouldRetry]면 한 번만 더 부른다. 취소는 그대로 위로 올라간다. */
internal suspend fun <R> retryOnce(attempt: suspend () -> R, shouldRetry: (R) -> Boolean): R {
    val first = attempt()
    return if (shouldRetry(first)) attempt() else first
}

/**
 * 다시 받으면 나아질 수 있는 실패인지.
 * - 응답을 못 받은 실패(시간 초과 `SocketTimeoutException`, 연결 끊김 등 [IOException]) → 다시 받는다.
 * - 500, 502, 503 → 다시 받는다.
 * - 403/404(파일 없음)는 다시 받아도 같다. 504는 오프라인일 때 Coil이 캐시만 찾다 내는 응답이라 다시 받지 않는다.
 * - 그 밖(디코딩 실패 등)도 다시 받지 않는다.
 */
internal fun isRetryableImageFailure(throwable: Throwable): Boolean {
    if (throwable is HttpException) return throwable.response.code in RETRYABLE_HTTP_CODES
    return throwable is IOException
}

private val RETRYABLE_HTTP_CODES = setOf(500, 502, 503)
