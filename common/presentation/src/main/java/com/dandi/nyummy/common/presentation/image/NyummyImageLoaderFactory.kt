package com.dandi.nyummy.common.presentation.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.disk.DiskCache
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

/**
 * 앱 전역에서 하나만 쓰는 Coil [ImageLoader] 를 구성합니다.
 *
 * Application 이 `SingletonImageLoader.Factory` 에서 이 팩토리를 호출하면
 * 모든 `AsyncImage` 가 같은 로더(같은 메모리/디스크 캐시)를 공유합니다.
 * 기본값에 맡기지 않고 캐시를 명시하며, S3 presigned URL 의 서명 쿼리가
 * 발급마다 바뀌어 캐시가 전혀 맞지 않는 문제를 [PresignedUrlKeyer] 로 바로잡습니다.
 * 요청마다 로딩 시간과 출처(메모리 캐시, 디스크, 네트워크)를 [ImageLoadReport] 로 남깁니다.
 * 앱이 만든 CDN `.webp` 주소는 서버에 없으면 같은 경로의 `.png` 로 다시 받습니다([CdnWebpFallbackInterceptor]).
 * CDN 그림이 시간 초과 같은 일시적인 이유로 실패하면 한 번 더 받습니다([CdnImageRetryInterceptor]).
 * 네트워크는 [newImageHttpClient] 로 타임아웃을 API 클라이언트와 같은 30초로 맞춥니다(Coil 기본 OkHttpClient 는 10초).
 *
 * 디스크 캐시는 Coil 기본 전략이라 HTTP 캐시 헤더로 다시 확인하지 않고, 한 번 받은 주소는 밀려날 때까지 캐시를 씁니다.
 * 그래서 CDN 그림을 바꿀 때는 같은 키에 덮어쓰지 말고 새 키(예: `.png` → `.webp`)로 올려야 기존 사용자에게도 새 그림이 보입니다.
 */
object NyummyImageLoaderFactory {

    /** 앱 가용 메모리 대비 이미지 메모리 캐시 비율. */
    private const val MEMORY_CACHE_PERCENT = 0.25

    /** 디스크 캐시 상한. 식사 사진 원본이 커서 넉넉히 잡는다. */
    private const val DISK_CACHE_MAX_BYTES = 128L * 1024L * 1024L

    private const val DISK_CACHE_DIR = "image_cache"

    fun create(context: PlatformContext, loadReport: ImageLoadReport): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(DISK_CACHE_MAX_BYTES)
                    .build()
            }
            .components {
                // 인터셉터는 먼저 넣은 것이 바깥이다. 다시 받기가 폴백(.webp → .png)을 통째로 감싼다.
                add(CdnImageRetryInterceptor())
                add(CdnWebpFallbackInterceptor(CdnWebpFallback(SharedPreferencesCdnPngFallbackMemory(context))))
                add(PresignedUrlKeyer())
                // 처음 네트워크 요청 때 한 번 만든다(메인 스레드에서 만들지 않게 지연).
                add(OkHttpNetworkFetcherFactory(callFactory = { newImageHttpClient() }))
            }
            .eventListenerFactory(ImageLoadEventListener.factory(loadReport))
            .build()
}

/** 이미지 요청 타임아웃. API 클라이언트(`common:data` `NetworkModule`)와 같은 값이다. */
internal const val IMAGE_HTTP_TIMEOUT_SECONDS = 30L

/**
 * 이미지 전용 OkHttpClient. 연결, 읽기, 쓰기 타임아웃을 [IMAGE_HTTP_TIMEOUT_SECONDS] 로 둔다.
 * 읽기 타임아웃은 "데이터가 그만큼 안 들어오면" 실패라, 큰 그림을 느리게나마 받고 있으면 끊지 않는다.
 * 요청 전체 시간(callTimeout)은 API 클라이언트처럼 두지 않는다. 느린 망에서 큰 그림이 받는 도중 잘리면 처음부터 다시 받아야 해서다.
 */
internal fun newImageHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(IMAGE_HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(IMAGE_HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(IMAGE_HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

/**
 * S3 presigned URL 은 같은 사진이라도 서명 쿼리(X-Amz-Signature 등)가 발급마다 달라
 * 전체 URL 을 캐시 키로 쓰면 항상 캐시 미스가 납니다.
 * 서명 쿼리가 붙은 URL 에 한해 쿼리를 벗긴 경로만 키로 써서 같은 사진이 캐시에 맞게 합니다.
 *
 * 일반 URL(쿼리가 이미지 선택에 의미가 있을 수 있는)은 건드리지 않고 기본 키를 따릅니다.
 */
private class PresignedUrlKeyer : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String? {
        val query = data.query ?: return null
        if (!query.contains(PRESIGNED_QUERY_MARKER, ignoreCase = true)) return null
        return buildString {
            data.scheme?.let { append(it).append("://") }
            data.authority?.let(::append)
            data.path?.let(::append)
        }
    }

    private companion object {
        /** AWS SigV4 presigned URL 임을 나타내는 쿼리 파라미터. */
        const val PRESIGNED_QUERY_MARKER = "X-Amz-Signature"
    }
}
