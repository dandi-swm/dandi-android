package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/**
 * 냐미 디자인 시스템 텍스트. [style]은 `NyummyTheme.typography.*` 20개 스타일 중 하나만 넘긴다.
 * 크기나 굵기를 copy로 바꾸면 디자인의 텍스트 스타일과 어긋나므로 바꾸지 않는다.
 */
@Composable
fun NyummyText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = NyummyTheme.colors.content.primary,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
        style = style,
    )
}

/** 부분 스타일(굵게, 링크 등)이 필요한 텍스트용 [AnnotatedString] 오버로드. */
@Composable
fun NyummyText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = NyummyTheme.colors.content.primary,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyTextPreview() {
    NyummyTheme {
        val typography = NyummyTheme.typography
        Column {
            NyummyText("오늘도 냐미랑 냠냠!", typography.displayL)
            NyummyText("7일 연속 기록 달성", typography.displayM)
            NyummyText("오늘의 식사", typography.titleL)
            NyummyText("오늘 1개 기록했어요", typography.titleM)
            NyummyText("점심 비빔밥", typography.titleS)
            NyummyText("사진 한 장이면 기록 끝", typography.bodyL)
            NyummyText("칼로리와 탄단지는 참고로만 보여줄게요", typography.bodyM)
            NyummyText("12:30 AI 분석 완료", typography.bodyS, color = NyummyTheme.colors.content.tertiary)
            NyummyText("밥 주기", typography.labelL)
            NyummyText("다시 시도", typography.labelM)
            NyummyText("히스토리", typography.labelS)
            NyummyText("집사, 오늘 첫 끼는 뭐야?", typography.voiceM)
            NyummyText("1,240", typography.numberXl)
            NyummyText("1,350 kcal", typography.numberM)
        }
    }
}
