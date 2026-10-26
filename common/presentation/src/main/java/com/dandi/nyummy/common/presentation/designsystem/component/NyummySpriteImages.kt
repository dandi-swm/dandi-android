package com.dandi.nyummy.common.presentation.designsystem.component

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException

/** 스프라이트 시트 이미지를 받은 결과. 결과마다 어떤 시트를 받은 것인지 함께 둔다. */
internal sealed interface SpriteImages {
    data object Loading : SpriteImages
    class Failed(val sheets: ImmutableList<NyummySpriteSheet>) : SpriteImages
    class Loaded(val sheets: ImmutableList<NyummySpriteSheet>, val images: ImmutableList<ImageBitmap>) : SpriteImages
}

/**
 * [sheets]의 이미지를 모두 받아 [ImageBitmap]으로 돌려준다. 한 장이라도 받지 못하면 실패다.
 * 원격 시트는 앱 공용 이미지 로더의 메모리와 디스크 캐시를 그대로 쓴다. 캔버스에서 일부 영역을 잘라 그리므로 하드웨어 비트맵은 쓰지 않는다.
 * [sheets]가 바뀌면 다시 받으며, 받는 동안에는 [SpriteImages.Loading]이다.
 */
@Composable
internal fun rememberSpriteImages(sheets: ImmutableList<NyummySpriteSheet>): SpriteImages {
    val context = LocalContext.current
    val result by produceState<SpriteImages>(initialValue = SpriteImages.Loading, sheets) {
        value = SpriteImages.Loading
        val loader = SingletonImageLoader.get(context)
        value = try {
            coroutineScope {
                sheets.map { sheet -> async { loadSpriteImage(context, loader, sheet.source) } }.awaitAll()
            }.toImmutableList().let { SpriteImages.Loaded(sheets, it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SpriteImages.Failed(sheets)
        }
    }
    // 시트가 막 바뀐 컴포지션에는 이전 시트의 결과가 남아 있다. 지금 요청과 다른 결과는 받는 중으로 본다.
    return when (val current = result) {
        is SpriteImages.Loaded -> if (current.sheets == sheets) current else SpriteImages.Loading
        is SpriteImages.Failed -> if (current.sheets == sheets) current else SpriteImages.Loading
        SpriteImages.Loading -> current
    }
}

private suspend fun loadSpriteImage(context: Context, loader: ImageLoader, source: NyummySpriteSource): ImageBitmap =
    when (source) {
        is NyummySpriteSource.Resource -> ImageBitmap.imageResource(context.resources, source.resId)
        is NyummySpriteSource.Url -> {
            val result = loader.execute(spriteSheetRequest(context, source.url)) as? SuccessResult
                ?: error("sprite load failed: ${source.url}")
            result.image.toBitmap().asImageBitmap()
        }
    }

/**
 * 원격 시트 한 장의 요청. 미리 받을 때도 이 요청을 써야 그릴 때 같은 캐시를 찾는다.
 * 캔버스에서 일부 영역을 잘라 그리므로 하드웨어 비트맵은 쓰지 않는다.
 */
internal fun spriteSheetRequest(context: Context, url: String): ImageRequest =
    ImageRequest.Builder(context).data(url).allowHardware(false).build()

/**
 * 곧 재생할 시트를 미리 받아 앱 공용 이미지 캐시에 넣어 둔다. 동작이 바뀔 때 시트를 받느라 기다리지 않게 한다.
 * [urls]가 바뀔 때마다 한 번씩 요청한다.
 */
@Composable
fun NyummySpritePrefetch(urls: ImmutableList<String>) {
    val context = LocalContext.current
    LaunchedEffect(urls) {
        val loader = SingletonImageLoader.get(context)
        urls.forEach { url -> loader.enqueue(spriteSheetRequest(context, url)) }
    }
}
