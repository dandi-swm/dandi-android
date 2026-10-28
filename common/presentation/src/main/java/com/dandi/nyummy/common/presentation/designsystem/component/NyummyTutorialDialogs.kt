package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** 튜토리얼에서 배우는 것. 건너뛰기 확인에 "남은 안내"로 보인다. */
enum class NyummyTutorialLesson(@StringRes internal val label: Int) {
    RecordMeal(R.string.nyummy_tutorial_lesson_record),
    OpenBox(R.string.nyummy_tutorial_lesson_box),
    DecorateRoom(R.string.nyummy_tutorial_lesson_room),
    CatHealth(R.string.nyummy_tutorial_lesson_health),
}

/**
 * 튜토리얼 건너뛰기 확인(피그마 S1). 놀란 냐미가 카드 위로 고개를 내밀고, 남은 안내를 보여 주며 계속하도록 권한다.
 * "계속 배울래요"는 큰 주 버튼, "그래도 건너뛸게요"는 잘 누르지 않게 작고 흐린 텍스트 버튼이다(터치 영역은 48).
 * 바깥이나 뒤로 가기를 누르면 [onContinue]다.
 */
@Composable
fun NyummyTutorialSkipDialog(
    remaining: ImmutableList<NyummyTutorialLesson>,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    NyummyDialogWindow(onDismissRequest = onContinue) {
        TutorialSkipCard(remaining = remaining, onContinue = onContinue, onSkip = onSkip)
    }
}

@Composable
private fun TutorialSkipCard(
    remaining: ImmutableList<NyummyTutorialLesson>,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val title = stringResource(R.string.nyummy_tutorial_skip_title)
    TutorialDialogCard(
        expression = NyummyTutorialExpression.Surprise,
        guideSize = SkipGuideSize,
        title = title,
        body = stringResource(R.string.nyummy_tutorial_skip_body),
    ) {
        if (remaining.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.m))
                    .padding(horizontal = NyummyTheme.spacing.s16, vertical = NyummyTheme.spacing.s12),
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s10),
            ) {
                NyummyText(
                    text = stringResource(R.string.nyummy_tutorial_skip_remaining),
                    style = NyummyTheme.typography.labelS,
                    color = NyummyTheme.colors.content.brand,
                )
                remaining.forEachIndexed { index, lesson ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s10),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(LessonNumberSize)
                                .background(NyummyTheme.colors.bg.actionPrimary, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            NyummyText(
                                text = "${index + 1}",
                                style = NyummyTheme.typography.labelXs,
                                color = NyummyTheme.colors.content.onAction,
                            )
                        }
                        NyummyText(text = stringResource(lesson.label), style = NyummyTheme.typography.bodyM)
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            NyummyButton(
                text = stringResource(R.string.nyummy_tutorial_skip_continue),
                onClick = onContinue,
                size = NyummyButtonSize.L,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                modifier = Modifier
                    .heightIn(min = NyummyTheme.size.touchTarget)
                    .nyummyClickable(onClick = onSkip),
                contentAlignment = Alignment.Center,
            ) {
                NyummyText(
                    text = stringResource(R.string.nyummy_tutorial_skip_anyway),
                    style = NyummyTheme.typography.bodyS.copy(textDecoration = TextDecoration.Underline),
                    color = NyummyTheme.colors.content.quiet,
                    modifier = Modifier.padding(horizontal = NyummyTheme.spacing.s12),
                )
            }
        }
    }
}

/** 냐미 건강 이야기의 한 장. 체형 그림과 체형 게이지에서 칠할 칸이 정해져 있다. */
enum class NyummyHealthStoryPage(@DrawableRes internal val drawable: Int, internal val gaugeIndex: Int) {
    /** 꾸준히 기록하면 둘 다 건강하다. 보통 체형. */
    Steady(R.drawable.nyummy_tutorial_weight_0, gaugeIndex = 2),

    /** 끼니를 거르면 홀쭉해진다. 가장 마른 체형. */
    Starving(R.drawable.nyummy_tutorial_weight_m2, gaugeIndex = 0),

    /** 너무 많이 먹으면 통통해진다. 가장 통통한 체형. */
    Overeating(R.drawable.nyummy_tutorial_weight_p2, gaugeIndex = 4),
}

/**
 * 냐미 건강 이야기 한 장(피그마 H1~H3). 체형 그림, 다섯 칸 체형 게이지, 제목과 설명, 장 표시와 다음 버튼이 있다.
 * 건너뛸 수 없어 바깥이나 뒤로 가기로 닫히지 않는다. 마른 냐미도 슬프거나 아파 보이지 않게, 탓하는 문구 없이 쓴다.
 *
 * @param pageIndex 0부터 센 지금 장 번호. 장 표시와 "n/전체"에 쓴다.
 */
@Composable
fun NyummyHealthStoryDialog(
    page: NyummyHealthStoryPage,
    pageIndex: Int,
    title: String,
    body: String,
    buttonText: String,
    onNext: () -> Unit,
) {
    NyummyDialogWindow(onDismissRequest = {}, dismissible = false) {
        HealthStoryCard(page, pageIndex, title, body, buttonText, onNext)
    }
}

@Composable
private fun HealthStoryCard(
    page: NyummyHealthStoryPage,
    pageIndex: Int,
    title: String,
    body: String,
    buttonText: String,
    onNext: () -> Unit,
) {
    val pageCount = NyummyHealthStoryPage.entries.size
    val healthy = page.gaugeIndex == NormalGaugeIndex
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.l))
            .semantics { paneTitle = title }
            .padding(
                start = NyummyTheme.spacing.s24,
                end = NyummyTheme.spacing.s24,
                top = NyummyTheme.spacing.s24,
                bottom = NyummyTheme.spacing.s20,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s20),
    ) {
        NyummyText(
            text = stringResource(R.string.nyummy_health_story_chip, pageIndex + 1, pageCount),
            style = NyummyTheme.typography.labelS,
            color = NyummyTheme.colors.content.brand,
            modifier = Modifier
                .background(NyummyTheme.colors.bg.brandSubtle, RoundedCornerShape(NyummyTheme.radius.full))
                .padding(horizontal = NyummyTheme.spacing.s10, vertical = NyummyTheme.spacing.s4),
        )
        Box(
            modifier = Modifier
                .size(StageSize)
                .background(
                    if (healthy) NyummyTheme.colors.bg.brandSubtle else NyummyTheme.colors.bg.promoSubtle,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(painter = painterResource(page.drawable), contentDescription = null, modifier = Modifier.size(StageImageSize))
        }
        WeightGauge(activeIndex = page.gaugeIndex, activeColor = gaugeColor(healthy))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            NyummyText(text = title, style = NyummyTheme.typography.titleL, textAlign = TextAlign.Center)
            NyummyText(
                text = body,
                style = NyummyTheme.typography.bodyM,
                color = NyummyTheme.colors.content.secondary,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            PageDots(count = pageCount, current = pageIndex)
            NyummyButton(text = buttonText, onClick = onNext, size = NyummyButtonSize.L, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun gaugeColor(healthy: Boolean): Color =
    if (healthy) NyummyTheme.colors.bg.actionPrimary else NyummyTheme.colors.content.promo

/** 다섯 칸 체형 게이지. 지금 체형 칸과 이름만 칠한다. */
@Composable
private fun WeightGauge(activeIndex: Int, activeColor: Color) {
    val labels = listOf(
        R.string.nyummy_health_gauge_lean,
        R.string.nyummy_health_gauge_slim,
        R.string.nyummy_health_gauge_normal,
        R.string.nyummy_health_gauge_chubby,
        R.string.nyummy_health_gauge_plump,
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6)) {
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
            labels.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(GaugeHeight)
                        .background(
                            if (index == activeIndex) activeColor else NyummyTheme.colors.bg.surfaceSunken,
                            RoundedCornerShape(GaugeHeight / 2),
                        ),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4)) {
            labels.forEachIndexed { index, label ->
                val active = index == activeIndex
                NyummyText(
                    text = stringResource(label),
                    style = if (active) NyummyTheme.typography.labelS else NyummyTheme.typography.bodyS,
                    color = if (active) activeColor else NyummyTheme.colors.content.quiet,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s6), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .width(if (active) DotActiveWidth else DotSize)
                    .height(DotSize)
                    .background(
                        if (active) NyummyTheme.colors.bg.actionPrimary else NyummyTheme.colors.content.disabled,
                        RoundedCornerShape(DotSize / 2),
                    ),
            )
        }
    }
}

/**
 * 튜토리얼 마침(피그마 H4). 응원하는 냐미가 카드 위로 고개를 내밀고, 버튼을 누르면 튜토리얼이 끝난다.
 * 바깥이나 뒤로 가기로 닫히지 않는다.
 */
@Composable
fun NyummyTutorialFinishDialog(
    title: String,
    body: String,
    buttonText: String,
    onStart: () -> Unit,
) {
    NyummyDialogWindow(onDismissRequest = {}, dismissible = false) {
        TutorialDialogCard(
            expression = NyummyTutorialExpression.Cheer,
            guideSize = FinishGuideSize,
            title = title,
            body = body,
        ) {
            NyummyButton(text = buttonText, onClick = onStart, size = NyummyButtonSize.L, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 냐미가 카드 위로 고개를 내미는 튜토리얼 다이얼로그 카드. 냐미는 카드 앞에 그려 몸통 윗부분이 카드에 걸친다. */
@Composable
private fun TutorialDialogCard(
    expression: NyummyTutorialExpression,
    guideSize: Dp,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val overlap = guideSize * GuideOverlapRatio
    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(top = guideSize - overlap)
                .fillMaxWidth()
                .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.l))
                .semantics { paneTitle = title }
                .padding(
                    start = NyummyTheme.spacing.s24,
                    end = NyummyTheme.spacing.s24,
                    top = overlap + NyummyTheme.spacing.s20,
                    bottom = NyummyTheme.spacing.s16,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
            ) {
                NyummyText(text = title, style = NyummyTheme.typography.titleL, textAlign = TextAlign.Center)
                NyummyText(
                    text = body,
                    style = NyummyTheme.typography.bodyM,
                    color = NyummyTheme.colors.content.secondary,
                    textAlign = TextAlign.Center,
                )
            }
            content()
        }
        Image(painter = painterResource(expression.drawable), contentDescription = null, modifier = Modifier.size(guideSize))
    }
}

private const val NormalGaugeIndex = 2

/** 다이얼로그 카드 위로 나온 냐미가 카드 윗변 아래로 겹치는 비율. */
private const val GuideOverlapRatio = 0.36f
private val SkipGuideSize = 104.dp
private val FinishGuideSize = 112.dp
private val LessonNumberSize = 20.dp
private val StageSize = 180.dp
private val StageImageSize = 160.dp
private val GaugeHeight = 8.dp
private val DotSize = 6.dp
private val DotActiveWidth = 18.dp

@Preview(showBackground = true, backgroundColor = 0xFF9E9E9E, widthDp = 390)
@Composable
private fun TutorialSkipCardPreview() {
    NyummyTheme {
        Box(Modifier.padding(NyummyTheme.spacing.s32)) {
            TutorialSkipCard(
                remaining = persistentListOf(
                    NyummyTutorialLesson.RecordMeal,
                    NyummyTutorialLesson.OpenBox,
                    NyummyTutorialLesson.DecorateRoom,
                    NyummyTutorialLesson.CatHealth,
                ),
                onContinue = {},
                onSkip = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF9E9E9E, widthDp = 390)
@Composable
private fun HealthStoryCardPreview() {
    NyummyTheme {
        Box(Modifier.padding(NyummyTheme.spacing.s32)) {
            HealthStoryCard(
                page = NyummyHealthStoryPage.Starving,
                pageIndex = 1,
                title = "끼니를 거르면\n내가 홀쭉해져",
                body = "집사가 밥을 굶으면 나도 같이 굶어서\n점점 말라 가. 다시 챙겨 주면 금방 돌아와!",
                buttonText = "다음",
                onNext = {},
            )
        }
    }
}
