package com.dandi.nyummy.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * `밥 주기` 버튼만 보여주는 홈 화면입니다.
 *
 * 현재 홈 화면의 다른 콘텐츠는 표시하지 않습니다. 하단 메뉴와 화면 이동은 앱 공통 Shell이
 * 담당하며, 버튼을 눌렀을 때 실행할 동작만 호출하는 쪽에서 전달받습니다.
 *
 * @param modifier 화면의 크기와 배치 방식을 조정합니다.
 * @param onFeedClick `밥 주기` 버튼을 눌렀을 때 호출됩니다.
 */
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onFeedClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgSurfaceIvory),
    ) {
        HomeFeedButton(
            label = stringResource(R.string.home_feed_button),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = FeedButtonHorizontalInset,
                    end = FeedButtonHorizontalInset,
                    bottom = FeedButtonBottomGap,
                )
                .widthIn(max = FeedButtonWidth)
                .fillMaxWidth()
                .testTag(HomeFeedButtonTestTag),
            onClick = onFeedClick,
        )
    }
}

@Composable
private fun HomeFeedButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    NyummyButton(
        label = label,
        modifier = modifier.height(FeedButtonHeight),
        size = NyummyButtonSize.Large,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_home_feed_bowl),
                contentDescription = null,
                tint = DesignSystemThemeImpl.designSystemColor.contentIconLevel0,
            )
        },
        onClick = onClick,
    )
}

private val FeedButtonWidth = 310.dp
private val FeedButtonHeight = 60.dp
private val FeedButtonHorizontalInset = 40.dp
private val FeedButtonBottomGap = 4.dp
private const val HomeFeedButtonTestTag = "home_feed_button"

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePagePreview() {
    DesignSystemTheme {
        HomePage()
    }
}
