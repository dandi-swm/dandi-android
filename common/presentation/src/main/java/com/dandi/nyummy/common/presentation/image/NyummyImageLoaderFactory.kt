package com.dandi.nyummy.common.presentation.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.Uri
import coil3.disk.DiskCache
import coil3.key.Keyer
import coil3.memory.MemoryCache
import coil3.request.Options
import okio.Path.Companion.toOkioPath

/**
 * 앱 전역에서 하나만 쓰는 Coil [ImageLoader] 를 구성합니다.
 *
 * Application 이 `SingletonImageLoader.Factory` 에서 이 팩토리를 호출하면
 * 모든 `AsyncImage` 가 같은 로더(같은 메모리/디스크 캐시)를 공유합니다.
 * 기본값에 맡기지 않고 캐시를 명시하며, S3 presigned URL 의 서명 쿼리가
 * 발급마다 바뀌어 캐시가 전혀 맞지 않는 문제를 [PresignedUrlKeyer] 로 바로잡습니다.
 * 요청마다 로딩 시간과 출처(메모리 캐시, 디스크, 네트워크)를 [ImageLoadReport] 로 남깁니다.
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
                add(PresignedUrlKeyer())
            }
            .eventListenerFactory(ImageLoadEventListener.factory(loadReport))
            .build()
}

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
