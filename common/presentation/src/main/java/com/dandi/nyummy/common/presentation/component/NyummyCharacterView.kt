package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow

/**
 * 캐릭터가 하는 말 한 마디.
 *
 * @property title 강조 첫 줄(세이지 색). 없으면 본문만 그린다.
 * @property body 본문.
 */
@Immutable
data class NyummySpeech(
    val title: String? = null,
    val body: String,
)

/**
 * 냐미 캐릭터 + 머리 위 말풍선 조합 컴포넌트.
 *
 * [NyummySpriteView] 는 스프라이트만 그리는 부품으로 유지하고,
 * 대사(말풍선)가 필요한 화면은 이 컴포넌트를 사용한다.
 * 말풍선은 캐릭터 위에 꼬리가 달린 형태로 붙어, 캐릭터가 어디에 배치되든 함께 움직인다.
 *
 * @param sheet 재생할 스프라이트 시트.
 * @param speech 말풍선 내용. null 이면 캐릭터만 그린다.
 * @param displayWidth 캐릭터 표시 폭(dp).
 * @param contentDescription 캐릭터 접근성 설명.
 */
@Composable
fun NyummyCharacterView(
    sheet: NyummySpriteSheet,
    modifier: Modifier = Modifier,
    speech: NyummySpeech? = null,
    displayWidth: Dp = DesignSystemThemeImpl.designSystemSize.characterHome,
    playing: Boolean = true,
    flipX: Boolean = false,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (speech != null) {
            NyummySpeechBubble(speech = speech)
            Spacer(modifier = Modifier.height(BubbleToCharacterGap))
        }
        NyummySpriteView(
            sheet = sheet,
            displayWidth = displayWidth,
            playing = playing,
            flipX = flipX,
            contentDescription = contentDescription,
        )
    }
}

/**
 * 아래로 꼬리가 달린 말풍선. 캐릭터 머리 위에 붙는 용도다.
 */
@Composable
fun NyummySpeechBubble(
    speech: NyummySpeech,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius22)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = BubbleMaxWidth)
                .designSystemDropShadow(
                    shape = shape,
                    shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
                ),
            shape = shape,
            color = colors.bgSurfaceIvory,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = BubbleHorizontalPadding,
                    vertical = BubbleVerticalPadding,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (speech.title != null) {
                    DandiText(
                        text = speech.title,
                        color = colors.contentAccentSage,
                        style = DesignSystemThemeImpl.typeScale.voiceRegularM,
                    )
                }
                DandiText(
                    text = speech.body,
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.voiceRegularM,
                    maxLines = 2,
                )
            }
        }
        // 꼬리: 말풍선과 같은 색의 아래로 향한 둥근 삼각형.
        // bias -1f=왼쪽 끝, 0f=가운데, 1f=오른쪽 끝 — -0.5f로 왼쪽 1/4 지점에 둔다.
        Canvas(
            modifier = Modifier
                .align(BiasAlignment.Horizontal(BubbleTailHorizontalBias))
                .size(BubbleTailWidth, BubbleTailHeight),
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
            drawPath(path = path, color = colors.bgSurfaceIvory)
        }
    }
}

private val BubbleMaxWidth = 280.dp
private val BubbleHorizontalPadding = 18.dp
private val BubbleVerticalPadding = 12.dp
private val BubbleTailWidth = 18.dp
private val BubbleTailHeight = 9.dp
private const val BubbleTailHorizontalBias = -0.5f
private val BubbleToCharacterGap = 2.dp

@Preview(showBackground = true)
@Composable
private fun NyummySpeechBubblePreview() {
    DesignSystemTheme {
        NyummySpeechBubble(
            speech = NyummySpeech(
                title = "첫 기록을 기다려요",
                body = "첫 끼를 기록하면 같이 챙겨볼게!",
            ),
        )
    }
}
