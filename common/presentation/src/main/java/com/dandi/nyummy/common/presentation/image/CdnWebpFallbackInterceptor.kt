package com.dandi.nyummy.common.presentation.image

import android.content.Context
import android.content.SharedPreferences
import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 공용 [coil3.ImageLoader]에 끼우는 인터셉터. 앱이 만든 CDN `.webp` 주소를 [CdnWebpFallback]으로 받는다.
 * 모든 `AsyncImage`, 미리 받기가 이 로더를 거치므로 화면마다 고치지 않아도 상점, 홈 방, 마이룸, 우편, 상자, 식사 연출이 함께 바뀐다.
 *
 * 받는 주소를 요청 자체에서 바꾸므로 메모리 캐시와 디스크 캐시 키도 실제로 받은 주소(`.webp` 또는 `.png`)가 된다.
 * 두 확장자의 그림은 서로 다른 캐시 칸에 들어가 섞이지 않는다.
 * 폴백까지 끝난 결과 하나만 화면에 돌아가므로 성공 콜백(방 배치 비율 계산 등)과 실패 콜백(빈칸, 기본 방)은 기존과 같이 한 번만 돈다.
 */
internal class CdnWebpFallbackInterceptor(
    private val fallback: CdnWebpFallback,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data as? String ?: return chain.proceed()
        return fallback.load(
            url = url,
            attempt = { target, networkAllowed ->
                if (target == url && networkAllowed) {
                    chain.proceed()
                } else {
                    val rewritten = request.newBuilder()
                        .data(target)
                        .apply { if (!networkAllowed) networkCachePolicy(CachePolicy.DISABLED) }
                        .build()
                    chain.withRequest(rewritten).proceed()
                }
            },
            outcomeOf = ImageResult::toCdnLoadOutcome,
        )
    }
}

/** Coil 결과를 폴백 갈래로 나눈다. 실패는 HTTP 코드만 보고 [cdnFailureOutcomeOf]로 나눈다. */
internal fun ImageResult.toCdnLoadOutcome(): CdnLoadOutcome = when (this) {
    is SuccessResult -> CdnLoadOutcome.SUCCESS
    is ErrorResult -> cdnFailureOutcomeOf((throwable as? HttpException)?.response?.code)
}

/**
 * [CdnPngFallbackMemory]를 SharedPreferences에 적는다. 앱을 다시 켜도 `.webp`가 없는 경로를 또 묻지 않게 한다.
 * 인터셉터는 메인 스레드에서 돌 수 있어 파일을 처음 읽을 때만 [ioDispatcher]로 넘긴다. 그 뒤로는 메모리에서 읽는다.
 */
internal class SharedPreferencesCdnPngFallbackMemory(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : CdnPngFallbackMemory {

    private val appContext = context.applicationContext

    @Volatile
    private var loaded: SharedPreferences? = null

    private suspend fun prefs(): SharedPreferences = loaded ?: withContext(ioDispatcher) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also {
            it.all // 파일을 다 읽을 때까지 여기서 기다린다. 이후 get은 막히지 않는다.
            loaded = it
        }
    }

    override suspend fun pngRememberedAt(pngUrl: String): Long? =
        prefs().getLong(pngUrl, NOT_REMEMBERED).takeIf { it != NOT_REMEMBERED }

    override suspend fun rememberPng(pngUrl: String, atMillis: Long) {
        prefs().edit().putLong(pngUrl, atMillis).apply()
    }

    override suspend fun forget(pngUrl: String) {
        prefs().edit().remove(pngUrl).apply()
    }

    private companion object {
        const val PREFS_NAME = "cdn_png_fallback"
        const val NOT_REMEMBERED = Long.MIN_VALUE
    }
}
