package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.R

/**
 * 음식 아이콘 식별자에 해당하는 CDN 이미지를 그립니다.
 * 식별자가 비어 있으면(일일/상세 API 미제공) 임시로 로컬 밥 아이콘을 보여줍니다.
 *
 * @param sizeFraction 슬롯 대비 아이콘이 차지하는 비율. 캘린더 셀처럼 아이콘을
 * 슬롯 가득 보여줘야 하는 곳에서 키워 쓴다. 폴백 밥 PNG 는 캔버스의 75% 만
 * 그림이라 같은 크기로 보이도록 비율을 보정한다.
 */
@Composable
internal fun HistoryFoodIcon(
    foodIconId: String,
    modifier: Modifier = Modifier,
    sizeFraction: Float = FOOD_ICON_SLOT_FRACTION,
) {
    if (foodIconId.isBlank()) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.nyummy_food_rice),
            contentDescription = null,
            modifier = modifier.fillMaxSize(
                (sizeFraction * FALLBACK_ICON_SCALE).coerceAtMost(1f),
            ),
            filterQuality = FilterQuality.None,
        )
        return
    }
    // CDN 404·오프라인 시 슬롯이 비어 보이지 않도록 로컬 밥 아이콘으로 대체한다.
    AsyncImage(
        model = FOOD_ICON_URL_FORMAT.format(foodIconId),
        contentDescription = null,
        modifier = modifier.fillMaxSize(sizeFraction),
        contentScale = ContentScale.Fit,
        error = painterResource(R.drawable.nyummy_food_rice),
    )
}

private const val FOOD_ICON_URL_FORMAT = "https://cdn.nyummy.co.kr/icons/%s.jpeg"

/** 이전 로컬 아이콘이 캔버스의 절반가량만 차지하던 크기감에 맞춘 슬롯 대비 기본 비율. */
private const val FOOD_ICON_SLOT_FRACTION = 0.5f

/** 밥 PNG(그림이 캔버스의 75%)를 CDN 아이콘과 같은 크기로 보이게 하는 보정 배율. */
private const val FALLBACK_ICON_SCALE = 1.34f
