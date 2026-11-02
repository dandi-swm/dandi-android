package com.dandi.nyummy.shop.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyRichText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.shop.entity.BannerCornerStyle
import com.dandi.nyummy.shop.entity.ShopActionVO
import com.dandi.nyummy.shop.entity.ShopBannerVO
import com.dandi.nyummy.shop.presentation.R
import kotlinx.collections.immutable.ImmutableList

/**
 * 추천 탭 맨 위 배너 묶음(피그마 `v2 / Section / HeroBanner`). 가로로 넘기고, 둘 이상이면 오른쪽 아래에 "n / m"을 보인다.
 */
@Composable
fun ShopHeroBanner(
    banners: ImmutableList<ShopBannerVO>,
    onAction: (ShopActionVO) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { banners.size }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = NyummyTheme.spacing.gutter),
        pageSpacing = NyummyTheme.spacing.s12,
    ) { page ->
        ShopBanner(
            banner = banners[page],
            onAction = onAction,
            index = if (banners.size > 1) stringResource(R.string.shop_banner_index, page + 1, banners.size) else null,
        )
    }
}

/**
 * 배너 한 장. 글자 없는 7:4 그림 위에 왼쪽부터 흐려지는 막을 깔고 서버 서식 제목·설명을 얹는다. 가격이 있으면 왼쪽
 * 아래 흰 가격표를 보인다. [banner]의 액션이 있으면 누를 수 있다. 프로모션 화면 맨 위 배너는 각진 모양이다.
 *
 * @param index 오른쪽 아래 "n / m". null이면 그리지 않는다
 */
@Composable
fun ShopBanner(
    banner: ShopBannerVO,
    onAction: (ShopActionVO) -> Unit,
    modifier: Modifier = Modifier,
    index: String? = null,
) {
    val shape = when (banner.cornerStyle) {
        BannerCornerStyle.ROUNDED -> RoundedCornerShape(NyummyTheme.radius.l)
        BannerCornerStyle.SQUARE -> RectangleShape
    }
    val action = banner.action
    val surface = NyummyTheme.colors.bg.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(BannerAspectRatio)
            .clip(shape)
            .then(if (action != null) Modifier.nyummyClickable(onClick = { onAction(action) }) else Modifier),
    ) {
        AsyncImage(
            model = banner.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to surface.copy(alpha = ScrimStartAlpha),
                        ScrimMidStop to surface.copy(alpha = ScrimMidAlpha),
                        ScrimEndStop to surface.copy(alpha = 0f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(NyummyTheme.spacing.s20),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6),
        ) {
            NyummyRichText(richText = banner.title)
            if (banner.description.isNotEmpty()) NyummyRichText(richText = banner.description)
        }
        if (banner.price.isNotEmpty()) {
            NyummyRichText(
                richText = banner.price,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(NyummyTheme.spacing.s20)
                    .background(surface, RoundedCornerShape(NyummyTheme.radius.full))
                    .padding(horizontal = NyummyTheme.spacing.s12, vertical = NyummyTheme.spacing.s8),
            )
        }
        if (index != null) {
            NyummyText(
                text = index,
                style = NyummyTheme.typography.labelS,
                color = NyummyTheme.colors.content.onAction,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = NyummyTheme.spacing.s12, bottom = NyummyTheme.spacing.s16)
                    .background(NyummyTheme.colors.content.primary, RoundedCornerShape(NyummyTheme.radius.full))
                    .padding(horizontal = NyummyTheme.spacing.s10, vertical = NyummyTheme.spacing.s4),
            )
        }
    }
}

/** 배너 그림 비율(7:4). */
private const val BannerAspectRatio = 7f / 4f

/** 글자 막: 왼쪽 85% → 42% 지점 60% → 62% 지점 투명(피그마 Text Scrim). */
private const val ScrimStartAlpha = 0.85f
private const val ScrimMidAlpha = 0.6f
private const val ScrimMidStop = 0.42f
private const val ScrimEndStop = 0.62f
