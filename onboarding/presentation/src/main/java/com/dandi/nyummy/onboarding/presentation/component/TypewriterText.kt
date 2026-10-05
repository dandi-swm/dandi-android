package com.dandi.nyummy.onboarding.presentation.component

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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.dandi.nyummy.common.presentation.component.DandiText
import kotlinx.coroutines.delay

private const val TypewriterCharDelayMillis = 38L

/**
 * 미연시 대사처럼 한 글자씩 찍히는 텍스트.
 *
 * 아직 안 나온 글자는 투명하게 미리 깔아 두어 타이핑 중에도 줄바꿈·높이가 바뀌지 않는다.
 * [revealed] 가 true 가 되면(탭으로 건너뛰기) 즉시 전부 보여준다. 타이핑이 스스로 끝나면 [onRevealed] 를 부른다.
 *
 * @param lineKey 같은 문장이 연달아 나와도 새 대사로 인식하도록 대사 위치를 키로 받는다.
 */
@Composable
internal fun TypewriterText(
    text: String,
    lineKey: Any,
    revealed: Boolean,
    onRevealed: () -> Unit,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val currentOnRevealed by rememberUpdatedState(onRevealed)
    var visibleCount by remember(lineKey) { mutableIntStateOf(0) }

    LaunchedEffect(lineKey, revealed) {
        if (revealed) return@LaunchedEffect
        while (visibleCount < text.length) {
            delay(TypewriterCharDelayMillis)
            visibleCount++
        }
        currentOnRevealed()
    }

    val shown = if (revealed) text.length else visibleCount.coerceAtMost(text.length)
    DandiText(
        text = buildAnnotatedString {
            append(text.substring(0, shown))
            withStyle(SpanStyle(color = Color.Transparent)) { append(text.substring(shown)) }
        },
        modifier = modifier,
        color = color,
        maxLines = Int.MAX_VALUE,
        style = style,
    )
}
