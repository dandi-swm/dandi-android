package com.dandi.nyummy.common.presentation.image

import android.util.Log
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

/** 이미지 종류. 주소 모양으로 나눈다. 콘솔 필터에 쓰므로 값 종류를 늘리지 않는다. */
enum class ImageKind(val value: String) {
    FOOD_ICON("food_icon"),
    MEAL_PHOTO("meal_photo"),
    CAT_SPRITE("cat_sprite"),
    OTHER_REMOTE("other_remote"),
    LOCAL("local"),
    ;

    companion object {
        /** 요청 데이터(주소, 파일, 리소스)에서 종류를 정한다. */
        fun of(data: Any?): ImageKind {
            val text = data?.toString().orEmpty()
            return when {
                !text.isHttpUrl() -> LOCAL
                text.contains(PRESIGNED_QUERY_MARKER, ignoreCase = true) -> MEAL_PHOTO
                text.contains(FOOD_ICON_PATH) -> FOOD_ICON
                text.contains(CAT_SPRITE_PATH) -> CAT_SPRITE
                else -> OTHER_REMOTE
            }
        }

        /** 스킴은 대소문자를 가리지 않는다. `http-image` 같은 문자열은 원격으로 보지 않는다. */
        private fun String.isHttpUrl(): Boolean =
            startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

        private const val PRESIGNED_QUERY_MARKER = "X-Amz-Signature"
        private const val FOOD_ICON_PATH = "/icons/"
        private const val CAT_SPRITE_PATH = "/cats/"
    }
}

/** 이미지를 어디서 가져왔는지. Coil 의 DataSource 와 같다. */
enum class ImageSource(val value: String) {
    MEMORY_CACHE("memory_cache"),
    MEMORY("memory"),
    DISK("disk"),
    NETWORK("network"),
}

/** 이미지 요청 하나의 측정. [finish] 는 요청당 한 번 부른다. */
interface ImageLoadTrace {
    fun finish(source: ImageSource?, success: Boolean)
}

/**
 * 이미지 로딩 시간의 sink. 빌드 타입에 따라 [DebugImageLoadReport] / [RemoteImageLoadReport] 중 하나가 주입된다.
 * 요청 시작부터 이미지가 준비될 때까지(바로 화면에 그려지는 시점)를 잰다.
 */
interface ImageLoadReport {
    fun start(kind: ImageKind): ImageLoadTrace
}

/** Debug 빌드용 sink. 한 줄로 Log.d 에 남긴다. */
@Singleton
class DebugImageLoadReport @Inject constructor() : ImageLoadReport {
    override fun start(kind: ImageKind): ImageLoadTrace {
        val startNanos = System.nanoTime()
        return object : ImageLoadTrace {
            override fun finish(source: ImageSource?, success: Boolean) {
                val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
                Log.d(TAG, "kind=${kind.value} source=${source?.value ?: "-"} success=$success ${elapsedMs}ms")
            }
        }
    }

    private companion object {
        const val TAG = "ImageLoad"
        const val NANOS_PER_MILLI = 1_000_000L
    }
}

/**
 * Release 빌드용 sink. 요청 시작 시각만 기기에서 재 두고, 이미지가 준비되면 보낼지 정한 뒤에야
 * Firebase Performance 커스텀 트레이스 `image_load` 를 만든다. 로딩 시간은 metric [LOAD_MS_METRIC] 에 담고
 * attribute(kind, source, result)를 넣어 바로 닫는다(트레이스 자체의 길이는 쓰지 않는다).
 *
 * - 트레이스를 요청 시작 때 열지 않는다. 열면 SDK 가 앱 상태와 세션을 등록하는데, 취소되거나 표본에서 빠진 요청은
 *   닫지 못해 그 등록이 요청마다 남는다. 보낼 요청만 끝에서 열고 닫는다.
 * - 메모리 캐시 적중은 아주 많아서 [MEMORY_CACHE_SAMPLE_RATE] 비율만 보낸다. 시간 분포를 보는 데는 충분하고,
 *   다 보내면 SDK 의 전송량 제한에 걸려 TTI 같은 다른 트레이스가 버려질 수 있다.
 * - Firebase 를 쓸 수 없거나 전송이 실패하면 보내지 않는다(NoOp). 이미지 로딩에는 영향이 없다.
 */
@Singleton
class RemoteImageLoadReport internal constructor(
    private val sampler: () -> Double,
) : ImageLoadReport {

    @Inject
    constructor() : this(sampler = { Math.random() })

    private val performance: FirebasePerformance? by lazy {
        runCatching { FirebasePerformance.getInstance() }.getOrNull()
    }

    override fun start(kind: ImageKind): ImageLoadTrace {
        val startNanos = System.nanoTime()
        return object : ImageLoadTrace {
            override fun finish(source: ImageSource?, success: Boolean) {
                if (!shouldSend(source, sampler)) return
                val perf = performance ?: return
                val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
                runCatching {
                    val trace: Trace = perf.newTrace(TRACE_NAME)
                    trace.putAttribute("kind", kind.value)
                    trace.putAttribute("source", source?.value ?: "none")
                    trace.putAttribute("result", if (success) "success" else "error")
                    trace.start()
                    trace.putMetric(LOAD_MS_METRIC, elapsedMs)
                    trace.stop()
                }
            }
        }
    }

    internal companion object {
        const val TRACE_NAME = "image_load"
        const val LOAD_MS_METRIC = "load_ms"
        private const val NANOS_PER_MILLI = 1_000_000L
        const val MEMORY_CACHE_SAMPLE_RATE = 0.1

        fun shouldSend(source: ImageSource?, sampler: () -> Double): Boolean =
            source != ImageSource.MEMORY_CACHE || sampler() < MEMORY_CACHE_SAMPLE_RATE
    }
}

private object NoOpImageLoadTrace : ImageLoadTrace {
    override fun finish(source: ImageSource?, success: Boolean) = Unit
}

/** 아무것도 남기지 않는 sink. 계측을 쓸 수 없을 때 쓴다. */
object NoOpImageLoadReport : ImageLoadReport {
    override fun start(kind: ImageKind): ImageLoadTrace = NoOpImageLoadTrace
}
