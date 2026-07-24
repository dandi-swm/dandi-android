package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.dandi.nyummy.history.presentation.model.foodIconResOf

/**
 * 음식 아이콘 식별자를 픽셀 아이콘 이미지로 그립니다.
 * 픽셀 아트가 뭉개지지 않도록 필터 없이 확대합니다.
 */
@Composable
internal fun HistoryFoodIcon(
    foodIconId: String,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = ImageBitmap.imageResource(foodIconResOf(foodIconId)),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        filterQuality = FilterQuality.None,
    )
}
