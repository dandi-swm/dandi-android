package com.dandi.nyummy.common.presentation.image

/** 그림 한 번 받기의 결과를 폴백 판단에 쓰는 갈래로 나눈 것. */
internal enum class CdnLoadOutcome {
    /** 받아서 그릴 수 있다. */
    SUCCESS,

    /** 서버에 그 파일이 없다(CDN 403, 404). 같은 경로의 다른 확장자를 받아 볼 근거가 된다. */
    MISSING,

    /** 그 밖의 실패(오프라인, 시간 초과, 5xx, 깨진 파일 등). 파일이 없다는 뜻이 아니다. */
    FAILED,
}

/**
 * 실패한 받기의 갈래. [httpCode]는 HTTP 응답 코드이고, 응답을 못 받은 실패(연결 끊김 등)는 null이다.
 *
 * CDN(CloudFront + S3)은 없는 키에 404가 아니라 403(AccessDenied)을 준다(`curl -I https://cdn.nyummy.co.kr/items/1.webp`로 확인).
 * 그래서 403과 404를 "파일 없음"으로 본다. 오프라인이면 Coil이 캐시만 찾다 504를 주고, 연결 실패는 코드가 없으므로 여기 들지 않는다.
 */
internal fun cdnFailureOutcomeOf(httpCode: Int?): CdnLoadOutcome =
    if (httpCode == 403 || httpCode == 404) CdnLoadOutcome.MISSING else CdnLoadOutcome.FAILED

/**
 * "이 경로는 서버에 `.webp`가 없다(403/404)"는 기억. 키는 `.png` 주소, 값은 확인한 시각(epoch millis)이다.
 * `.png`를 받는 데 성공했는지와는 따로 적는다. 느린 망에서 `.png` 받기가 시간 초과로 실패해도
 * 다시 그릴 때 `.webp` 403 왕복을 또 하지 않게 하기 위해서다. `.webp`가 있는 경로는 기본값이므로 적지 않는다.
 */
internal interface CdnPngFallbackMemory {
    /** [pngUrl] 경로에 `.webp`가 없다고 적은 시각. 적은 적 없으면 null. */
    suspend fun webpMissingAt(pngUrl: String): Long?

    suspend fun rememberWebpMissing(pngUrl: String, atMillis: Long)

    suspend fun forget(pngUrl: String)
}

/**
 * CDN 그림을 `.webp`로 먼저 받고, 서버에 없으면 같은 경로의 `.png`로 다시 받는다.
 * Coil에 묶이지 않은 판단만 여기 두고, 실제로 받는 일은 [CdnWebpFallbackInterceptor]가 [load]의 `attempt`로 넘긴다.
 *
 * - `.webp`가 없다(403/404, [CdnLoadOutcome.MISSING]) → 그 자리에서 [memory]에 적고 `.png`를 네트워크로 받는다.
 *   [PNG_MEMORY_TTL_MILLIS] 동안은 `.webp`를 묻지 않고 곧장 `.png`로 간다(매번 실패 요청을 두 번 치지 않게).
 *   `.png`가 시간 초과로 실패해도 기억은 남아, 다시 그릴 때(자동 재시도 포함) `.png`만 받는다.
 * - 그 밖의 실패(오프라인 등, [CdnLoadOutcome.FAILED]) → 파일이 없다는 근거가 아니므로 아무것도 적지 않는다.
 *   업데이트 전에 받아 둔 `.png`가 디스크 캐시에 있으면 그것만 꺼내 보여 준다(네트워크는 다시 타지 않음).
 * - 기억이 지나면 `.webp`를 다시 묻는다. 서버가 `.webp`로 바꾼 뒤에는 늦어도 이 시간 안에 새 파일로 옮겨 간다.
 */
internal class CdnWebpFallback(
    private val memory: CdnPngFallbackMemory,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    /**
     * [url]을 받는다. 앱이 만든 CDN `.webp` 주소가 아니면 [attempt]를 그대로 한 번 부른다.
     *
     * @param attempt 주소 하나를 받는다. `networkAllowed`가 false면 디스크 캐시에서만 꺼낸다.
     * @param outcomeOf [attempt] 결과의 갈래.
     * @return 마지막으로 쓸 결과. 모두 실패하면 실패 결과를 그대로 돌려 화면의 기존 실패 처리가 돈다.
     */
    suspend fun <R> load(
        url: String,
        attempt: suspend (url: String, networkAllowed: Boolean) -> R,
        outcomeOf: (R) -> CdnLoadOutcome,
    ): R {
        val pngUrl = NyummyCdn.pngFallbackUrlOf(url) ?: return attempt(url, true)

        val rememberedAt = memory.webpMissingAt(pngUrl)
        if (rememberedAt != null && isFresh(rememberedAt)) {
            val png = attempt(pngUrl, true)
            if (outcomeOf(png) != CdnLoadOutcome.MISSING) return png
            // 기억과 달리 `.png`가 없다(서버가 옛 키를 지움). 기억을 버리고 `.webp`로 간다.
            memory.forget(pngUrl)
            return attempt(url, true)
        }

        val webp = attempt(url, true)
        return when (outcomeOf(webp)) {
            CdnLoadOutcome.SUCCESS -> {
                // 서버가 `.webp`로 바꿨다. 지난 기억이 남아 있으면 치운다.
                if (rememberedAt != null) memory.forget(pngUrl)
                webp
            }

            CdnLoadOutcome.MISSING -> {
                // 403/404는 서버에 파일이 없다는 확실한 답이라 `.png` 결과를 기다리지 않고 바로 적는다.
                memory.rememberWebpMissing(pngUrl, nowMillis())
                attempt(pngUrl, true)
            }

            CdnLoadOutcome.FAILED -> {
                val cachedPng = attempt(pngUrl, false)
                if (outcomeOf(cachedPng) == CdnLoadOutcome.SUCCESS) cachedPng else webp
            }
        }
    }

    /** 시계가 뒤로 간 경우(기억한 시각이 미래)도 지난 것으로 보고 다시 묻는다. */
    private fun isFresh(rememberedAt: Long): Boolean {
        val age = nowMillis() - rememberedAt
        return age in 0 until PNG_MEMORY_TTL_MILLIS
    }

    companion object {
        /** `.webp`가 없는 경로를 기억하는 시간. 서버가 `.webp`로 바꾼 뒤 앱이 옮겨 가는 최대 지연이기도 하다. */
        const val PNG_MEMORY_TTL_MILLIS: Long = 24L * 60L * 60L * 1000L
    }
}
