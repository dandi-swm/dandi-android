package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.entity.tag.TagTone
import com.dandi.nyummy.common.entity.tag.TagVO
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

/** 태그를 그리는 자리. 데이터는 같고 모양만 다르다. */
enum class NyummyTagStyle {
    /** 상품 그림 왼쪽 위, 목록 줄 이름 옆. 모서리 8dp, 굵은 글자. */
    Item,

    /** 배너 제목 위. 알약 모양. */
    Banner,
}

/** 서버가 보낸 태그([TagVO])를 [style] 자리의 모양으로 그린다. */
@Composable
fun NyummyTag(
    tag: TagVO,
    modifier: Modifier = Modifier,
    style: NyummyTagStyle = NyummyTagStyle.Item,
) {
    val bg = NyummyTheme.colors.bg
    val content = NyummyTheme.colors.content
    // Neutral 바탕은 상품 그림 바탕(surfaceSunken) 위에서도 보이게 한 단계 진하게 쓴다.
    val (container, color) = when (tag.tone) {
        TagTone.PROMO -> bg.promoSubtle to content.promo
        TagTone.SUCCESS -> bg.successSubtle to content.success
        TagTone.NEUTRAL -> bg.actionNeutral to content.secondary
    }
    val shapeModifier = when (style) {
        NyummyTagStyle.Item -> Modifier
            .background(container, RoundedCornerShape(NyummyTheme.radius.xs))
            .padding(horizontal = NyummyComponentDimens.TagItemHorizontalPadding, vertical = NyummyTheme.spacing.s2)
        NyummyTagStyle.Banner -> Modifier
            .defaultMinSize(minHeight = NyummyComponentDimens.TagBannerMinHeight)
            .background(container, RoundedCornerShape(NyummyTheme.radius.full))
            .padding(
                horizontal = NyummyComponentDimens.TagBannerHorizontalPadding,
                vertical = NyummyComponentDimens.TagBannerVerticalPadding,
            )
    }
    val textStyle = when (style) {
        NyummyTagStyle.Item -> NyummyTheme.typography.labelSStrong
        NyummyTagStyle.Banner -> NyummyTheme.typography.labelS
    }
    Box(modifier = modifier.then(shapeModifier), contentAlignment = Alignment.Center) {
        NyummyText(text = tag.text, style = textStyle, color = color, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun NyummyTagPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier
                .background(NyummyTheme.colors.bg.surfaceSunken)
                .padding(NyummyTheme.spacing.s16),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        ) {
            NyummyTagStyle.entries.forEach { style ->
                Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
                    NyummyTag(TagVO("한정", TagTone.PROMO), style = style)
                    NyummyTag(TagVO("새 집사 전용", TagTone.SUCCESS), style = style)
                    NyummyTag(TagVO("신상품", TagTone.NEUTRAL), style = style)
                }
            }
        }
    }
}
