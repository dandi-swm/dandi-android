package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.image.NyummyFoodIcons

/**
 * 음식 픽셀 아이콘. 식별자(서버 iconId)와 같은 번호의 앱 리소스를 놓인 자리 안에 그린다([NyummyFoodIcons]). 네트워크로 받지 않는다.
 * 식별자가 비어 있거나(분석 전이거나 실패한 식사) 앱에 없는 번호면 [placeholder]를 그리고,
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
    val drawable = NyummyFoodIcons.drawableOf(foodIconId)
    if (drawable == null) {
        placeholder?.invoke()
        return
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = null,
        modifier = modifier.fillMaxSize(sizeFraction),
        contentScale = ContentScale.Fit,
    )
}

@Preview(showBackground = true)
@Composable
private fun NyummyFoodIconPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyMealRow(title = "유효한 아이콘", subtitle = "오전 8:10", leading = { NyummyFoodIcon(foodIconId = "12") })
            NyummyMealRow(title = "빈 식별자", subtitle = "오후 12:24", leading = { NyummyFoodIcon(foodIconId = "") })
            NyummyMealRow(
                title = "앱에 없는 번호 + placeholder",
                subtitle = "오후 3:40",
                leading = { NyummyFoodIcon(foodIconId = "9999", placeholder = { NyummyMealRowPlaceholder() }) },
            )
            NyummyMealRow(
                title = "빈 식별자 + placeholder",
                subtitle = "오후 7:02",
                leading = { NyummyFoodIcon(foodIconId = "", placeholder = { NyummyMealRowPlaceholder() }) },
            )
        }
    }
}
