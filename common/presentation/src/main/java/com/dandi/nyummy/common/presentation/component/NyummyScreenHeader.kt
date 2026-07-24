package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 뒤로 가기 + 대형 타이틀로 구성된 화면 헤더.
 *
 * 배경과 테두리를 가진 표면형 앱바인 [NyummyTopAppBar]와 달리, 화면 배경 위에 투명하게
 * 얹혀 디스플레이 타이포로 제목을 보여준다. 뒤로 갈 수 있는 상세/폼 화면의 기본 상단 헤더로 사용한다.
 */
@Composable
fun NyummyScreenHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String = BackContentDescription,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyIconButton(
            contentDescription = backContentDescription,
            onClick = onBackClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.chevron_left_24px),
                contentDescription = null,
                tint = DesignSystemThemeImpl.designSystemColor.contentIconLevel0,
            )
        }
        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
        DandiText(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
    }
}

private const val BackContentDescription = "뒤로 가기"

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun NyummyScreenHeaderPreview() {
    DesignSystemTheme {
        NyummyScreenHeader(
            title = "식사 기록",
            onBackClick = {},
        )
    }
}
