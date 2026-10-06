package com.dandi.nyummy.intro.presentation

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import kotlin.math.min
import kotlin.math.roundToInt
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 인트로 스플래시. 키친 스테이지 배경 위에 로고와 헤드라인, 바구니 든 냐미, 음식 스티커, 진행 카드를 얹는다.
 *
 * 진행바는 실제 진행률이 없는 장식이다. 권한 안내([isPermissionNoticeVisible])가 떠 있는 동안은
 * 0에 멈춰 있다가 닫히면 90%까지 차오르고, 시작 게이트가 끝나면([isComplete]) 100%까지 빠르게 채운다.
 *
 * 해상도 대응(360×640부터 태블릿까지 배경이 잘리지 않게):
 * - 배경은 화면 폭에 맞추고 아래에 붙인다. 남는 위쪽은 배경 상단과 같은 흰색이다.
 * - 냐미는 스테이지 바닥선(배경 높이의 73.5%)에 발을 맞추고 크기는 min(폭 × 0.64, 300)이다.
 *   제목 아래 공간이 모자라면 줄이고, 120보다 작아지면(큰 글꼴 등) 숨긴다.
 * - 화면 높이가 [CompactHeight]보다 낮으면 제목 블록을 작게 쓴다(로고 120×70, 제목 26, 위 여백 24).
 * - 스티커는 냐미 중심 기준 상대 좌표로 두고, 제목이나 진행 카드와 겹치거나 화면 밖이면 숨긴다.
 */
@Composable
fun IntroSplashContent(
    isComplete: Boolean,
    isPermissionNoticeVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    var decorativeTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { decorativeTarget = SplashProgressTarget }
    val progress by animateFloatAsState(
        targetValue = when {
            isComplete -> 1f
            isPermissionNoticeVisible -> 0f
            else -> decorativeTarget
        },
        animationSpec = tween(
            durationMillis = if (isComplete) SplashCompleteDurationMillis else SplashProgressDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "IntroSplashProgress",
    )

    val theme = NyummyTheme
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    val gutterDp = theme.spacing.gutter
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val compact = windowHeight < CompactHeight
    val titleTopGap = if (compact) theme.spacing.s24 else theme.spacing.s32
    val cardBottomGap = theme.spacing.s24
    val heroTitleGap = theme.spacing.s8
    val background = painterResource(R.drawable.intro_bg_kitchen)
    val backgroundRatio = background.intrinsicSize.height / background.intrinsicSize.width

    Layout(
        modifier = modifier
            .fillMaxSize()
            .background(theme.colors.bg.canvas),
        content = {
            Image(
                painter = background,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.layoutId(SplashSlot.Background),
            )
            SplashTitleBlock(compact = compact, modifier = Modifier.layoutId(SplashSlot.Title))
            Image(
                painter = painterResource(R.drawable.intro_hero_basket),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.layoutId(SplashSlot.Hero),
            )
            SplashStickers.forEach { sticker ->
                FoodSticker(
                    iconRes = sticker.iconRes,
                    rotation = sticker.rotation,
                    modifier = Modifier.layoutId(sticker),
                )
            }
            SplashProgressCard(
                progress = progress,
                isComplete = isComplete,
                modifier = Modifier.layoutId(SplashSlot.Card),
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gutter = gutterDp.roundToPx()
        val loose = Constraints(maxWidth = width - gutter * 2)

        val backgroundHeight = (width * backgroundRatio).roundToInt()
        val backgroundTop = height - backgroundHeight
        val floorY = backgroundTop + backgroundHeight * StageFloorRatio

        val title = measurables.first { it.layoutId == SplashSlot.Title }.measure(loose)
        val titleTop = topInset + titleTopGap.roundToPx()
        val titleRect = Rect(
            left = ((width - title.width) / 2).toFloat(),
            top = titleTop.toFloat(),
            right = ((width + title.width) / 2).toFloat(),
            bottom = (titleTop + title.height).toFloat(),
        )

        val cardWidth = min(width - gutter * 2, CardMaxWidth.roundToPx())
        val card = measurables.first { it.layoutId == SplashSlot.Card }.measure(Constraints.fixedWidth(cardWidth))
        val cardTop = height - bottomInset - cardBottomGap.roundToPx() - card.height
        val cardRect = Rect(
            left = ((width - cardWidth) / 2).toFloat(),
            top = cardTop.toFloat(),
            right = ((width + cardWidth) / 2).toFloat(),
            bottom = (cardTop + card.height).toFloat(),
        )

        // 냐미 크기: 기본 min(폭 × 0.64, 300). 실제로 잰 제목 아래 공간에 맞춰 줄인다.
        // 큰 글꼴처럼 제목이 길어져 [HeroHideBelow]보다 작아지면 겹치지 않도록 냐미와 스티커를 숨긴다.
        val titleGap = heroTitleGap.toPx()
        val availableForHero = (floorY - (titleRect.bottom + titleGap)) / HeroFootRatio
        val heroSize = min(min(width * HeroWidthRatio, HeroMaxSize.toPx()), availableForHero)
        val showHero = heroSize >= HeroHideBelow.toPx()
        val heroPx = heroSize.coerceAtLeast(0f).roundToInt()
        val heroTop = (floorY - heroSize * HeroFootRatio).roundToInt()
        val heroLeft = (width - heroPx) / 2
        val hero = measurables.first { it.layoutId == SplashSlot.Hero }.measure(Constraints.fixed(heroPx, heroPx))
        val heroCenterX = heroLeft + heroPx / 2f
        val heroCenterY = heroTop + heroPx / 2f
        val stickerScale = heroSize / HeroReferenceSize.toPx()

        val background = measurables.first { it.layoutId == SplashSlot.Background }
            .measure(Constraints.fixed(width, backgroundHeight))

        val stickers = SplashStickers.map { sticker ->
            val placeable = measurables.first { it.layoutId == sticker }.measure(Constraints())
            val centerX = heroCenterX + sticker.offsetX.toPx() * stickerScale
            val centerY = heroCenterY + sticker.offsetY.toPx() * stickerScale
            val rect = Rect(
                left = centerX - placeable.width / 2f,
                top = centerY - placeable.height / 2f,
                right = centerX + placeable.width / 2f,
                bottom = centerY + placeable.height / 2f,
            )
            val visible = showHero && rect.left >= 0 && rect.right <= width &&
                !rect.overlaps(titleRect) && !rect.overlaps(cardRect)
            Triple(placeable, rect, visible)
        }

        layout(width, height) {
            background.place(0, backgroundTop)
            if (showHero) hero.place(heroLeft, heroTop)
            stickers.forEach { (placeable, rect, visible) ->
                if (visible) placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
            }
            title.place(titleRect.left.roundToInt(), titleTop)
            card.place(cardRect.left.roundToInt(), cardTop)
        }
    }
}

private enum class SplashSlot { Background, Title, Hero, Card }

/** 냐미(250dp일 때) 중심에서 스티커 중심까지의 거리. 냐미 크기가 바뀌면 같은 비율로 늘고 준다. */
private enum class SplashSticker(
    @DrawableRes val iconRes: Int,
    val offsetX: Dp,
    val offsetY: Dp,
    val rotation: Float,
) {
    FriedEgg(CommonR.drawable.nyummy_food_smooth_fried_egg, (-137).dp, (-141).dp, 8f),
    Salad(CommonR.drawable.nyummy_food_smooth_salad, 136.dp, (-161).dp, -7f),
    Rice(CommonR.drawable.nyummy_food_smooth_rice, (-144).dp, (-8).dp, -6f),
    Gimbap(CommonR.drawable.nyummy_food_smooth_gimbap, 148.dp, (-20).dp, 6f),
    Sandwich(CommonR.drawable.nyummy_food_smooth_sandwich, (-157).dp, 117.dp, 4f),
    Bibimbap(CommonR.drawable.nyummy_food_smooth_bibimbap, 125.dp, 100.dp, -5f),
}

private val SplashStickers = SplashSticker.entries

@Composable
private fun SplashTitleBlock(compact: Boolean, modifier: Modifier = Modifier) {
    val theme = NyummyTheme
    val headline = if (compact) theme.typography.displayM else theme.typography.displayL
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) theme.spacing.s8 else theme.spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(CommonR.drawable.nyummy_brand_logo),
            contentDescription = stringResource(R.string.intro_splash_logo_description),
            modifier = if (compact) Modifier.size(CompactLogoWidth, CompactLogoHeight) else Modifier.size(LogoWidth, LogoHeight),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NyummyText(
                text = stringResource(R.string.intro_splash_headline_line1),
                style = headline,
                textAlign = TextAlign.Center,
            )
            NyummyText(
                text = stringResource(R.string.intro_splash_headline_line2),
                style = headline,
                color = theme.colors.content.brand,
                textAlign = TextAlign.Center,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(SubtitleIconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_heart),
                contentDescription = null,
                tint = theme.colors.content.brand,
                modifier = Modifier.size(theme.size.iconS),
            )
            NyummyText(
                text = stringResource(R.string.intro_splash_bubble),
                style = theme.typography.bodyM,
                color = theme.colors.content.secondary,
            )
        }
    }
}

/** 흰 원 + soft 그림자 위의 음식 일러스트(36). 조금씩 기울여 스티커처럼 보이게 한다. */
@Composable
private fun FoodSticker(
    @DrawableRes iconRes: Int,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    val theme = NyummyTheme
    Box(
        modifier = modifier
            .rotate(rotation)
            .size(StickerSize)
            .nyummyShadow(CircleShape, theme.elevation.soft)
            .background(theme.colors.bg.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(StickerFoodSize),
        )
    }
}

@Composable
private fun SplashProgressCard(
    progress: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = NyummyTheme
    val shape = RoundedCornerShape(theme.radius.l)
    Column(
        modifier = modifier
            .nyummyShadow(shape, theme.elevation.soft)
            .background(theme.colors.bg.surface, shape)
            .padding(horizontal = theme.spacing.s20, vertical = theme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(CardContentGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = stringResource(
                    if (isComplete) R.string.intro_splash_progress_complete else R.string.intro_splash_progress_label,
                ),
                style = theme.typography.titleS,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 큰 글꼴에서도 퍼센트는 한 줄로 두고, 줄바꿈은 왼쪽 문구가 맡는다.
            NyummyText(
                text = stringResource(R.string.intro_splash_progress_percent, (progress * 100).toInt()),
                style = theme.typography.numberM,
                color = theme.colors.content.brand,
                maxLines = 1,
                modifier = Modifier.padding(start = theme.spacing.s8),
            )
        }
        NyummyLinearProgress(progress = progress, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(theme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_lightbulb),
                contentDescription = null,
                tint = theme.colors.content.warning,
                modifier = Modifier.size(theme.size.iconXs),
            )
            NyummyText(
                text = stringResource(R.string.intro_splash_tip),
                style = theme.typography.bodyS,
                color = theme.colors.content.tertiary,
            )
        }
    }
}

/** 진행바 장식 애니메이션의 목표치. 게이트 완료 전에는 90%에서 멈춘다. */
private const val SplashProgressTarget = 0.9f
private const val SplashProgressDurationMillis = 1500

/** 게이트 완료 시 100% 채움 시간. IntroViewModel의 이동 대기(500ms)보다 짧아야 한다. */
private const val SplashCompleteDurationMillis = 250

/** 키친 배경에서 스테이지(선반) 바닥선의 높이 비율. */
private const val StageFloorRatio = 0.735f

/** 히어로 이미지에서 냐미 발끝이 있는 높이 비율. 이 높이를 스테이지 바닥선에 맞춘다. */
private const val HeroFootRatio = 0.968f
private const val HeroWidthRatio = 0.64f
private val HeroMaxSize = 300.dp
private val HeroHideBelow = 120.dp

/** 스티커 거리(SplashSticker)를 잰 기준 냐미 크기. */
private val HeroReferenceSize = 250.dp

private val LogoWidth = 154.dp
private val LogoHeight = 90.dp
private val CompactLogoWidth = 120.dp
private val CompactLogoHeight = 70.dp

/** 이보다 낮은 화면(360×640 등)은 제목 블록을 작게 써서 냐미가 제목에 가리지 않게 한다. */
private val CompactHeight = 720.dp
private val SubtitleIconGap = 6.dp
private val StickerSize = 56.dp
private val StickerFoodSize = 36.dp
private val CardContentGap = 10.dp
/** 카드 최대 너비. 태블릿에서도 카드가 가운데에 적당한 폭으로 놓인다. */
private val CardMaxWidth = 480.dp

@Preview(name = "Intro Splash 390×844", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IntroSplashContentPreview() {
    NyummyTheme { IntroSplashContent(isComplete = false, isPermissionNoticeVisible = false) }
}

@Preview(name = "Intro Splash 360×640", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun IntroSplashContentSmallPreview() {
    NyummyTheme { IntroSplashContent(isComplete = false, isPermissionNoticeVisible = false) }
}

@Preview(name = "Intro Splash 600×960", showBackground = true, widthDp = 600, heightDp = 960)
@Composable
private fun IntroSplashContentTabletPreview() {
    NyummyTheme { IntroSplashContent(isComplete = true, isPermissionNoticeVisible = false) }
}
