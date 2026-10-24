package com.dandi.nyummy.common.presentation.helper

import android.content.Context
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import com.dandi.nyummy.common.domain.helper.ImagePrefetchHelper
import com.dandi.nyummy.common.presentation.designsystem.component.spriteSheetRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ImagePrefetchHelperImpl(private val context: Context) : ImagePrefetchHelper {

    override suspend fun prefetchSpriteSheets(urls: List<String>) {
        val loader = SingletonImageLoader.get(context)
        // 시트를 다 메모리에 올리면 이미지 메모리 캐시를 많이 차지하므로 디스크에만 남긴다.
        // 실패는 결과(ErrorResult)로만 돌아오므로 따로 잡지 않는다.
        coroutineScope {
            urls.map { url ->
                async {
                    val request = spriteSheetRequest(context, url).newBuilder()
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build()
                    loader.execute(request)
                }
            }.awaitAll()
        }
    }
}
