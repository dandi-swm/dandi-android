package com.dandi.nyummy.home.presentation.prepare

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.presentation.cat.catNameStringResource
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.home.presentation.R
import kotlin.math.min
import kotlin.math.roundToInt
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 로그인 직후 냐미 방 준비 화면. 냐미 애니메이션과 시트 그림을 받는 동안 보이고, 끝나면 홈으로 넘어간다.
 */
@Composable
fun HomePreparePage(
    viewModel: HomePrepareViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomePrepareContent(isComplete = uiState.isComplete)
}

/**
 * 인트로 스플래시와 같은 짜임이다. 위에 제목, 가운데에 방을 정리하는 냐미 그림, 아래에 진행 카드가 놓인다.
 *
 * 진행바는 실제 진행률이 없는 장식이다. 90%까지 차오른 뒤 기다리다가, 준비가 끝나면([isComplete]) 100%까지 빠르게 채운다.
 *
 * 해상도 대응: 배경은 화면 폭에 맞추고 아래에 붙인다. 남는 위쪽은 배경 위 끝과 같은 흰색이다.
 * 배경 위쪽은 비어 있어 제목이 그 위에 놓이고, 진행 카드는 바닥에 겹친다.
 * 창이 넓어(태블릿 가로, 분할 화면 등) 폭에 맞추면 냐미 머리가 제목에 가리거나 화면 밖으로 나가면,
 * 머리가 제목 바로 아래에 오도록 배경을 줄이고 양옆은 캔버스 색으로 둔다.
 * 화면 높이가 [CompactHeight]보다 낮으면 제목을 작게 쓴다.
 */
@Composable
internal fun HomePrepareContent(
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    var decorativeTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { decorativeTarget = ProgressTarget }
    val progress by animateFloatAsState(
        targetValue = if (isComplete) 1f else decorativeTarget,
        animationSpec = tween(
            durationMillis = if (isComplete) CompleteDurationMillis else ProgressDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "HomePrepareProgress",
    )
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    val gutterDp = NyummyTheme.spacing.gutter
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val compact = windowHeight < CompactHeight
    val titleTopGap = if (compact) NyummyTheme.spacing.s24 else NyummyTheme.spacing.s32
    val catTitleGap = NyummyTheme.spacing.s8
    val cardBottomGap = NyummyTheme.spacing.s24
    val backgroundPainter = painterResource(R.drawable.home_prepare_bg)
    val backgroundRatio = backgroundPainter.intrinsicSize.height / backgroundPainter.intrinsicSize.width

    Layout(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
        content = {
            Image(
                painter = backgroundPainter,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.layoutId(PrepareSlot.Background),
            )
            PrepareTitle(compact = compact, modifier = Modifier.layoutId(PrepareSlot.Title))
            PrepareProgressCard(
                progress = progress,
                isComplete = isComplete,
                modifier = Modifier.layoutId(PrepareSlot.Card),
            )
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val gutter = gutterDp.roundToPx()

        val title = measurables.first { it.layoutId == PrepareSlot.Title }
            .measure(Constraints(maxWidth = width - gutter * 2))
        val titleTop = topInset + titleTopGap.roundToPx()

        // 배경 높이: 폭에 맞춘 높이와, 아래에 붙였을 때 냐미 머리([CatTopRatio])가 제목 아래에 오는 가장 큰 높이 중 작은 쪽.
        // 실제로 잰 제목 높이를 쓰므로 큰 글꼴이나 짧은 창에서도 머리가 가리지 않는다.
        val catTopLimit = titleTop + title.height + catTitleGap.roundToPx()
        val heightKeepingCat = (height - catTopLimit) / (1f - CatTopRatio)
        val backgroundHeight = min(width * backgroundRatio, heightKeepingCat).coerceAtLeast(0f).roundToInt()
        val backgroundWidth = (backgroundHeight / backgroundRatio).roundToInt()
        val background = measurables.first { it.layoutId == PrepareSlot.Background }
            .measure(Constraints.fixed(backgroundWidth, backgroundHeight))

        val cardWidth = min(width - gutter * 2, CardMaxWidth.roundToPx())
        val card = measurables.first { it.layoutId == PrepareSlot.Card }.measure(Constraints.fixedWidth(cardWidth))
        val cardTop = height - bottomInset - cardBottomGap.roundToPx() - card.height

        layout(width, height) {
            background.place((width - backgroundWidth) / 2, height - backgroundHeight)
            title.place((width - title.width) / 2, titleTop)
            card.place((width - cardWidth) / 2, cardTop)
        }
    }
}

@Composable
private fun PrepareTitle(compact: Boolean, modifier: Modifier = Modifier) {
    val headline = if (compact) NyummyTheme.typography.displayM else NyummyTheme.typography.displayL
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) NyummyTheme.spacing.s8 else NyummyTheme.spacing.s12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NyummyText(
                text = catNameStringResource(R.string.home_prepare_headline_line1),
                style = headline,
                textAlign = TextAlign.Center,
            )
            NyummyText(
                text = stringResource(R.string.home_prepare_headline_line2),
                style = headline,
                color = NyummyTheme.colors.content.brand,
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
                tint = NyummyTheme.colors.content.brand,
                modifier = Modifier.size(NyummyTheme.size.iconS),
            )
            NyummyText(
                text = stringResource(R.string.home_prepare_subtitle),
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
            )
        }
    }
}

@Composable
private fun PrepareProgressCard(
    progress: Float,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(NyummyTheme.radius.l)
    Column(
        modifier = modifier
            .nyummyShadow(shape, NyummyTheme.elevation.soft)
            .background(NyummyTheme.colors.bg.surface, shape)
            .padding(horizontal = NyummyTheme.spacing.s20, vertical = NyummyTheme.spacing.s16),
        verticalArrangement = Arrangement.spacedBy(CardContentGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyText(
                text = stringResource(
                    if (isComplete) R.string.home_prepare_progress_complete else R.string.home_prepare_progress_label,
                ),
                style = NyummyTheme.typography.titleS,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 큰 글꼴에서도 퍼센트는 한 줄로 두고, 줄바꿈은 왼쪽 문구가 맡는다.
            NyummyText(
                text = stringResource(R.string.home_prepare_progress_percent, (progress * 100).toInt()),
                style = NyummyTheme.typography.numberL,
                color = NyummyTheme.colors.content.brand,
                maxLines = 1,
                modifier = Modifier.padding(start = NyummyTheme.spacing.s8),
            )
        }
        NyummyLinearProgress(progress = progress, modifier = Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_lightbulb),
                contentDescription = null,
                tint = NyummyTheme.colors.content.promo,
                modifier = Modifier.size(NyummyTheme.size.iconXs),
            )
            NyummyText(
                text = catNameStringResource(R.string.home_prepare_tip),
                style = NyummyTheme.typography.bodyS,
                color = NyummyTheme.colors.content.tertiary,
            )
        }
    }
}

/** 진행바 장식 애니메이션의 목표치. 준비가 끝나기 전에는 90%에서 멈춘다. */
private const val ProgressTarget = 0.9f
private const val ProgressDurationMillis = 1500

/** 준비 완료 시 100% 채움 시간. HomePrepareViewModel의 이동 대기(500ms)보다 짧아야 한다. */
private const val CompleteDurationMillis = 250

/** 배경에서 냐미 머리(귀 끝)가 시작하는 높이 비율. 원본 1536px 중 625px 지점(40.7%)보다 조금 위로 잡는다. */
private const val CatTopRatio = 0.4f

/** 이보다 낮은 화면(360×640 등)은 제목을 작게 쓴다. 인트로와 같은 기준이다. */
private val CompactHeight = 720.dp
private val SubtitleIconGap = 6.dp
private val CardContentGap = 10.dp

/** 카드 최대 너비. 태블릿에서도 카드가 가운데에 적당한 폭으로 놓인다. */
private val CardMaxWidth = 480.dp

private enum class PrepareSlot { Background, Title, Card }

@Preview(name = "Home Prepare 390×844", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePrepareContentPreview() {
    NyummyTheme { HomePrepareContent(isComplete = false) }
}

@Preview(name = "Home Prepare 360×640", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun HomePrepareContentSmallPreview() {
    NyummyTheme { HomePrepareContent(isComplete = false) }
}

@Preview(name = "Home Prepare 600×960", showBackground = true, widthDp = 600, heightDp = 960)
@Composable
private fun HomePrepareContentTabletPreview() {
    NyummyTheme { HomePrepareContent(isComplete = true) }
}

@Preview(name = "Home Prepare 1280×800", showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun HomePrepareContentLandscapePreview() {
    NyummyTheme { HomePrepareContent(isComplete = false) }
}
