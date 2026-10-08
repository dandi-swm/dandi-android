package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.coroutines.delay

/** 한 글자가 찍히는 간격 */
const val NyummyTypewriterCharDelayMillis = 38L

/**
 * 미연시 대사처럼 한 글자씩 찍히는 텍스트.
 *
 * 아직 안 나온 글자는 투명하게 미리 깔아 두어 타이핑 중에도 줄바꿈과 높이(말풍선 크기)가 바뀌지 않는다.
 * 화면 읽기 프로그램에는 처음부터 문장 전체가 읽힌다.
 * [revealed]가 true가 되면(탭으로 건너뛰기) 즉시 전부 보여준다. 타이핑이 스스로 끝나면 [onRevealed]를 부른다.
 *
 * @param lineKey 바뀌면 처음부터 다시 찍는다. 같은 문장이 연달아 나와도 새 대사로 인식하게 하려면 대사 위치를 넘긴다.
 */
@Composable
fun NyummyTypewriterText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    lineKey: Any = text,
    revealed: Boolean = false,
    onRevealed: () -> Unit = {},
    color: Color = NyummyTheme.colors.content.primary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val currentOnRevealed by rememberUpdatedState(onRevealed)
    var visibleCount by remember(lineKey) { mutableIntStateOf(0) }

    LaunchedEffect(lineKey, revealed) {
        if (revealed) return@LaunchedEffect
        while (visibleCount < text.length) {
            delay(NyummyTypewriterCharDelayMillis)
            visibleCount++
        }
        currentOnRevealed()
    }

    val shown = if (revealed) text.length else visibleCount.coerceAtMost(text.length)
    NyummyText(
        text = buildAnnotatedString {
            append(text.substring(0, shown))
            withStyle(SpanStyle(color = Color.Transparent)) { append(text.substring(shown)) }
        },
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier,
    )
}
