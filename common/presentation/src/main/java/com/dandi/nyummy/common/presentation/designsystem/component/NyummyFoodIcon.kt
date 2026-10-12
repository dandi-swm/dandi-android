package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * 음식 픽셀 아이콘. 식별자에 해당하는 CDN 이미지를 놓인 자리 안에 그린다.
 * 식별자가 비어 있거나(분석 전이거나 실패한 식사) 이미지를 받지 못하면(404, 오프라인) 아무것도 그리지 않는다.
 * 다른 음식 아이콘으로 대신 채우면 사용자가 먹은 음식으로 잘못 읽기 때문이다.
 *
 * @param sizeFraction 놓인 자리 대비 아이콘이 차지하는 비율
 */
@Composable
fun NyummyFoodIcon(
    foodIconId: String,
    modifier: Modifier = Modifier,
    sizeFraction: Float = 1f,
) {
    var loadFailed by remember(foodIconId) { mutableStateOf(false) }
    if (foodIconId.isBlank() || loadFailed) return
    AsyncImage(
        model = FOOD_ICON_URL_FORMAT.format(foodIconId),
        contentDescription = null,
        modifier = modifier.fillMaxSize(sizeFraction),
        contentScale = ContentScale.Fit,
        onState = { state ->
            if (state is AsyncImagePainter.State.Error) loadFailed = true
        },
    )
}

private const val FOOD_ICON_URL_FORMAT = "https://cdn.nyummy.co.kr/icons/%s.png"
