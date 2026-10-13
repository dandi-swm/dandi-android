package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 음식 픽셀 아이콘. 식별자에 해당하는 CDN 이미지를 놓인 자리 안에 그린다.
 * 식별자가 비어 있거나(분석 전이거나 실패한 식사) 이미지를 아직 받지 못했거나 받지 못하면(403, 오프라인) [placeholder]를 그리고,
 * [placeholder]가 없으면 아무것도 그리지 않는다. 다른 음식 아이콘으로 대신 채우면 사용자가 먹은 음식으로 잘못 읽기 때문이다.
 *
 * @param sizeFraction 놓인 자리 대비 아이콘이 차지하는 비율
 * @param placeholder 아이콘을 보여 줄 수 없을 때 대신 그릴 내용
 */
@Composable
fun NyummyFoodIcon(
    foodIconId: String,
    modifier: Modifier = Modifier,
    sizeFraction: Float = 1f,
    placeholder: (@Composable () -> Unit)? = null,
) {
    var loaded by remember(foodIconId) { mutableStateOf(false) }
    var loadFailed by remember(foodIconId) { mutableStateOf(false) }
    if (!loaded) placeholder?.invoke()
    if (foodIconId.isBlank() || loadFailed) return
    AsyncImage(
        model = FOOD_ICON_URL_FORMAT.format(foodIconId),
        contentDescription = null,
        modifier = modifier.fillMaxSize(sizeFraction),
        contentScale = ContentScale.Fit,
        onState = { state ->
            loaded = state is AsyncImagePainter.State.Success
            if (state is AsyncImagePainter.State.Error) loadFailed = true
        },
    )
}

private const val FOOD_ICON_URL_FORMAT = "https://cdn.nyummy.co.kr/icons/%s.png"

@Preview(showBackground = true)
@Composable
private fun NyummyFoodIconPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyMealRow(title = "유효한 아이콘", subtitle = "오전 8:10", leading = { NyummyFoodIcon(foodIconId = "rice") })
            NyummyMealRow(title = "빈 식별자", subtitle = "오후 12:24", leading = { NyummyFoodIcon(foodIconId = "") })
            NyummyMealRow(
                title = "빈 식별자 + placeholder",
                subtitle = "오후 7:02",
                leading = { NyummyFoodIcon(foodIconId = "", placeholder = { NyummyMealRowPlaceholder() }) },
            )
        }
    }
}
