package com.dandi.nyummy.common.presentation.cutscene

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** 그림 한 장을 [coverSize](px) 를 빈틈없이 덮는 크기로 읽어 달라는 요청. */
data class CutsceneImageRequest(@DrawableRes val image: Int, val coverSize: Size)

/**
 * 컷신 그림을 백그라운드에서 화면에 그릴 크기로 줄여 읽습니다. 다 읽은 그림부터 [requests] 의 순서 번호로 들어옵니다.
 *
 * 그림은 밀도 폴더(mdpi~xxhdpi)에 WebP 로 두고, 기기 밀도에 맞는 한 벌을 골라 실제로 그릴 크기만큼만 메모리에 올립니다.
 * 고른 그림보다 크게 그릴 때는 그 크기 그대로 읽고 그릴 때 늘립니다. 컷신이 화면에서 사라지면 그림도 함께 놓아 줍니다.
 */
@Composable
fun rememberCutsceneImages(requests: List<CutsceneImageRequest>): Map<Int, ImageBitmap> {
    val resources = LocalContext.current.resources
    val images: SnapshotStateMap<Int, ImageBitmap> = remember(requests) { mutableStateMapOf() }
    LaunchedEffect(requests) {
        requests.forEachIndexed { index, request ->
            val bitmap = withContext(Dispatchers.IO) { decodeToCover(resources, request) }
            if (bitmap != null) images[index] = bitmap
        }
    }
    return images
}

private fun decodeToCover(resources: Resources, request: CutsceneImageRequest): ImageBitmap? {
    // 밀도 폴더 그림은 그냥 재면 기기 밀도로 늘리거나 줄인 크기가 나온다. 고른 그림의 실제 픽셀 크기를 잰다.
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inScaled = false
    }
    BitmapFactory.decodeResource(resources, request.image, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val scale = max(request.coverSize.width / bounds.outWidth, request.coverSize.height / bounds.outHeight).coerceAtMost(1f)
    // 밀도 비율로 읽으면 디코더가 읽으면서 줄여, 원본 크기 비트맵을 한 번 더 만들지 않는다.
    val options = BitmapFactory.Options().apply {
        inScaled = true
        inDensity = bounds.outWidth
        inTargetDensity = max(1, (bounds.outWidth * scale).roundToInt())
    }
    return BitmapFactory.decodeResource(resources, request.image, options)?.asImageBitmap()
}
