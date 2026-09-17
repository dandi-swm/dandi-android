package com.dandi.nyummy.history.presentation.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyBadge
import com.dandi.nyummy.common.presentation.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyMascot
import com.dandi.nyummy.common.presentation.component.NyummyMascotPose
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf

/**
 * AI 가 음식으로 인식하지 못한 식사 카드입니다.
 *
 * 이름도 열량도 없어 일반 식사 행으로 그리면 빈 카드가 되므로, 실패를 그대로 알려주고
 * 그 자리에서 재분석하거나 기록을 지울 수 있게 합니다.
 */
@Composable
internal fun HistoryMealFailedCard(
    meal: MealHistoryVO,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    StatusCardSurface(
        background = colors.bgWarningSoft,
        borderColor = colors.borderWarningDefault,
        modifier = modifier,
    ) {
        StatusCardHeader(
            recordedAt = meal.recordedAt,
            timeColor = colors.contentWarning,
            badgeLabel = stringResource(R.string.history_analysis_failed_badge),
            badgeTone = NyummyBadgeTone.Warning,
        )
        Spacer(Modifier.height(StatusCardHeaderGap))
        StatusCardBody(
            pose = NyummyMascotPose.Sleeping,
            title = stringResource(R.string.history_analysis_failed_title),
            description = stringResource(R.string.history_analysis_failed_body),
        )
        Spacer(Modifier.height(StatusCardActionGap))
        Row(horizontalArrangement = Arrangement.spacedBy(StatusCardActionSpacing)) {
            NyummyButton(
                label = stringResource(R.string.history_analysis_retry),
                modifier = Modifier.weight(1f),
                style = NyummyButtonStyle.Secondary,
                size = NyummyButtonSize.Medium,
                onClick = onRetry,
            )
            NyummyButton(
                label = stringResource(R.string.history_analysis_delete),
                style = NyummyButtonStyle.Ghost,
                size = NyummyButtonSize.Medium,
                onClick = onDelete,
            )
        }
    }
}

/**
 * 분석 결과를 기다리는 중인 식사 카드입니다.
 * 재분석을 방금 요청한 직후에도 같은 카드를 쓰고, 결과는 분석 완료 알림을 받아 갱신합니다.
 */
@Composable
internal fun HistoryMealAnalyzingCard(
    meal: MealHistoryVO,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    StatusCardSurface(
        background = colors.bgInfoSoft,
        borderColor = colors.borderInfoDefault,
        modifier = modifier,
    ) {
        StatusCardHeader(
            recordedAt = meal.recordedAt,
            timeColor = colors.contentInfo,
            badgeLabel = stringResource(R.string.history_analysis_pending_badge),
            badgeTone = NyummyBadgeTone.Neutral,
        )
        Spacer(Modifier.height(StatusCardHeaderGap))
        StatusCardBody(
            pose = NyummyMascotPose.Eating,
            title = stringResource(R.string.history_analysis_pending_title),
            description = stringResource(R.string.history_analysis_pending_body),
            bounceMascot = true,
        )
    }
}

/** 두 상태 카드가 공유하는 껍데기입니다. 일반 식사 행과 같은 모서리·여백을 씁니다. */
@Composable
private fun StatusCardSurface(
    background: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
        color = background,
        contentColor = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        border = BorderStroke(StatusCardBorderWidth, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = StatusCardInset,
                vertical = StatusCardVerticalInset,
            ),
            content = content,
        )
    }
}

/** 촬영 시각과 상태 배지를 좌우로 배치한 카드 머리입니다. */
@Composable
private fun StatusCardHeader(
    recordedAt: String,
    timeColor: Color,
    badgeLabel: String,
    badgeTone: NyummyBadgeTone,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DandiText(
            text = meridiemTimeOf(recordedAt),
            modifier = Modifier.weight(1f),
            color = timeColor,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        NyummyBadge(label = badgeLabel, tone = badgeTone)
    }
}

/** 냐미 일러스트 + 안내 문구로 이뤄진 카드 본문입니다. */
@Composable
private fun StatusCardBody(
    pose: NyummyMascotPose,
    title: String,
    description: String,
    bounceMascot: Boolean = false,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        NyummyMascot(
            pose = pose,
            contentDescription = null,
            modifier = Modifier
                .size(StatusCardMascotSize)
                .then(if (bounceMascot) Modifier.mascotBounce() else Modifier),
        )
        Spacer(Modifier.width(StatusCardMascotGap))
        Column(modifier = Modifier.weight(1f)) {
            DandiText(
                text = title,
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
            Spacer(Modifier.height(StatusCardTitleGap))
            DandiText(
                text = description,
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
        }
    }
}

/** 분석이 돌고 있다는 느낌만 주는 가벼운 상하 움직임입니다. */
@Composable
private fun Modifier.mascotBounce(): Modifier {
    val transition = rememberInfiniteTransition(label = "mealAnalyzingMascot")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -MascotBounceDistancePx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = MascotBounceDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mealAnalyzingMascotOffset",
    )
    return this.graphicsLayer { translationY = offset }
}

private val StatusCardBorderWidth = 1.dp
private val StatusCardInset = 16.dp
private val StatusCardVerticalInset = 12.dp
private val StatusCardHeaderGap = 10.dp
private val StatusCardMascotSize = 52.dp
private val StatusCardMascotGap = 12.dp
private val StatusCardTitleGap = 4.dp
private val StatusCardActionGap = 12.dp
private val StatusCardActionSpacing = 8.dp
private const val MascotBounceDistancePx = 6f
private const val MascotBounceDurationMillis = 700

private val previewFailedMeal = MealHistoryVO(
    id = "preview-failed",
    recordedAt = "12:30",
    orderIndex = 1,
    status = MealAnalysisStatus.FAILED,
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryMealFailedCardPreview() {
    DesignSystemTheme {
        HistoryMealFailedCard(
            meal = previewFailedMeal,
            onRetry = {},
            onDelete = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun HistoryMealAnalyzingCardPreview() {
    DesignSystemTheme {
        HistoryMealAnalyzingCard(
            meal = previewFailedMeal.copy(status = MealAnalysisStatus.ANALYZING),
            modifier = Modifier.padding(20.dp),
        )
    }
}
