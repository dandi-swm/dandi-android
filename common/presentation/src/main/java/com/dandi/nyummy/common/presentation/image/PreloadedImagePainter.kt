package com.dandi.nyummy.common.presentation.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size

/**
 * 그려지기 전에도 바로 불러오는 그림 페인터. 원본 크기로 요청해 자리 크기를 기다리지 않는다.
 * 연출 중에 그림을 바꿔 끼울 때(닫힌 상자 → 열린 상자, 브론즈 → 골드) 바뀌는 순간 비지 않게 미리 만들어 둔다.
 * 주소가 비어 있으면 아무것도 불러오지 않는다.
 */
@Composable
fun rememberPreloadedImagePainter(url: String): Painter {
    val context = LocalContext.current
    val request = remember(context, url) {
        ImageRequest.Builder(context)
            .data(url.ifEmpty { null })
            .size(Size.ORIGINAL)
            .build()
    }
    return rememberAsyncImagePainter(request)
}
