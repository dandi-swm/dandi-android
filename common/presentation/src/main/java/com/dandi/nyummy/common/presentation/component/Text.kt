package com.dandi.nyummy.common.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

@Composable
fun DandiText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = DesignSystemThemeImpl.typeScale.textRegularM,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textDecoration = textDecoration,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

/** 부분 스타일(밑줄 링크 등)이 필요한 텍스트용 [AnnotatedString] 오버로드. */
@Composable
fun DandiText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    style: TextStyle = DesignSystemThemeImpl.typeScale.textRegularM,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style,
    )
}
