package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.entity.richText.RichText
import com.dandi.nyummy.common.entity.richText.RichTextVO
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.foundation.accentSafe
import com.dandi.nyummy.common.presentation.designsystem.foundation.rememberAccentGlyphCoverage
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyContentColors
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTypography

/**
 * 서버가 보낸 꾸민 글자([RichText])를 조각 순서대로 이어 그린다.
 * 글자 조각은 typeScale, color 토큰 이름으로 스타일을 정하고, 아이콘 조각은 글자 사이에 넣는다.
 * 모르는 typeScale은 bodyM, 모르는 color는 contentPrimary로 그리고, 모르는 아이콘 조각은 뺀다.
 */
@Composable
fun NyummyRichText(
    richText: RichText,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val typography = NyummyTheme.typography
    val content = NyummyTheme.colors.content
    val density = LocalDensity.current
    val gap = NyummyTheme.spacing.s4
    val iconSize = NyummyTheme.size.iconXs
    val pieces = richText.filter { !it.isIcon || RichTextIcon.of(it.icon) != null }
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    val coverage = rememberAccentGlyphCoverage()

    val text = buildAnnotatedString {
        pieces.forEachIndexed { index, piece ->
            val icon = RichTextIcon.of(piece.icon)
            if (icon == null) {
                // 조각마다 스타일이 달라, 엄마까투리체에 없는 글자 처리도 조각 단위로 한다.
                val style = typography.styleOf(piece.typeScale).accentSafe(piece.text, coverage).toSpanStyle().copy(
                    color = content.colorOf(piece.color),
                    textDecoration = if (piece.strike) TextDecoration.LineThrough else null,
                )
                withStyle(style) { append(piece.text) }
                return@forEachIndexed
            }
            // 아이콘 옆 간격은 앞뒤에 조각이 있을 때만 넣는다.
            val size = if (icon == RichTextIcon.Coin) NyummyComponentDimens.RichTextCoinSize else iconSize
            val start = if (index > 0) gap else 0.dp
            val end = if (index < pieces.lastIndex) gap else 0.dp
            val id = "icon$index"
            inlineContent[id] = InlineTextContent(
                placeholder = with(density) {
                    Placeholder((start + size + end).toSp(), size.toSp(), PlaceholderVerticalAlign.TextCenter)
                },
            ) {
                Box(Modifier.fillMaxSize().padding(start = start, end = end)) {
                    RichTextIconImage(icon, tint = content.colorOf(piece.color), modifier = Modifier.size(size))
                }
            }
            appendInlineContent(id)
        }
    }
    // 크기가 다른 조각이 섞여도 줄이 겹치지 않게 줄 높이는 가장 큰 글자 스타일을 따른다.
    val baseStyle = pieces.filterNot { it.isIcon }
        .map { typography.styleOf(it.typeScale) }
        .maxByOrNull { it.lineHeight.value }
        ?: typography.bodyM

    NyummyText(
        text = text,
        style = baseStyle,
        modifier = modifier,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
    )
}

/** 꾸민 글자에 넣을 수 있는 아이콘. [tinted]면 조각의 color로 칠하고, 아니면 그림 색 그대로 그린다. */
private enum class RichTextIcon(val key: String, @DrawableRes val res: Int, val tinted: Boolean) {
    Coin("coin", R.drawable.nyummy_asset_coin, tinted = false),
    Clock("clock", R.drawable.nyummy_ic_clock, tinted = true),
    ChevronRight("chevronRight", R.drawable.nyummy_ic_chevron_right, tinted = true),
    ;

    companion object {
        fun of(key: String): RichTextIcon? = entries.find { it.key == key }
    }
}

@Composable
private fun RichTextIconImage(icon: RichTextIcon, tint: Color, modifier: Modifier) {
    val painter = painterResource(icon.res)
    if (icon.tinted) {
        Icon(painter = painter, contentDescription = null, tint = tint, modifier = modifier)
    } else {
        Image(painter = painter, contentDescription = null, modifier = modifier)
    }
}

/**
 * 서버 글자 토큰 이름을 테마 글자 스타일로 바꾼다. 모르는 이름이면 bodyM.
 * 서버 이름은 백엔드와의 계약이라, v2에서 합친 Strong 이름도 계속 받는다.
 */
internal fun NyummyTypography.styleOf(typeScale: String): TextStyle = when (typeScale) {
    "displayL" -> displayL
    "displayM" -> displayM
    "titleL" -> titleL
    "titleM" -> titleM
    "titleS" -> titleS
    "bodyL" -> bodyL
    "bodyS" -> bodyS
    "labelL" -> labelL
    "labelM" -> labelM
    "labelMStrong" -> labelM
    "labelS" -> labelS
    "labelSStrong" -> labelS
    "voiceM" -> voiceM
    "voiceS" -> voiceS
    "numberXl" -> numberXl
    "numberL" -> numberL
    "numberM" -> numberM
    "numberS" -> numberS
    "numberXs" -> numberXs
    else -> bodyM
}

/**
 * 서버 색 토큰 이름(contentPrimary 등)을 테마 글자색으로 바꾼다. 모르는 이름이면 contentPrimary.
 * 서버 이름은 백엔드와의 계약이라, 앱에서 없앤 역할의 이름도 v2의 가까운 색으로 받는다.
 */
internal fun NyummyContentColors.colorOf(color: String): Color = when (color) {
    "contentSecondary" -> secondary
    "contentTertiary" -> tertiary
    "contentDisabled" -> disabled
    "contentOnAction" -> onAction
    "contentOnInverse" -> onAction
    "contentBrand" -> brand
    "contentSuccess" -> brand
    "contentWarning" -> promo
    "contentDanger" -> danger
    "contentInfo" -> secondary
    "contentPromo" -> promo
    else -> primary
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun NyummyRichTextPreview() {
    NyummyTheme {
        Column(modifier = Modifier.padding(NyummyTheme.spacing.s16)) {
            NyummyRichText(listOf(RichTextVO("새 집사\n스타터 묶음", "displayM", "contentPrimary")))
            NyummyRichText(
                listOf(
                    RichTextVO("전체보기", "labelS", "contentSecondary"),
                    RichTextVO(icon = "chevronRight", color = "contentSecondary"),
                ),
            )
            NyummyRichText(
                listOf(
                    RichTextVO(icon = "clock", color = "contentTertiary"),
                    RichTextVO("9시간 후 바뀌어요", "labelS", "contentTertiary"),
                ),
            )
            NyummyRichText(
                listOf(
                    RichTextVO("20%", "numberS", "contentPromo"),
                    RichTextVO(icon = "coin"),
                    RichTextVO("280 ", "numberS", "contentPrimary"),
                    RichTextVO("350", "labelS", "contentTertiary", strike = true),
                ),
            )
        }
    }
}
