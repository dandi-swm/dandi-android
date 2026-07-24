package com.dandi.nyummy.common.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/** Canonical top app bar from Figma node `993:681`. */
@Composable
fun NyummyTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TopAppBarHeight),
        color = colors.bgDefaultLevel0,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(ComponentBorderWidth, colors.borderDefaultLevel1),
    ) {
        Row(
            modifier = Modifier.padding(TopAppBarPadding),
            horizontalArrangement = Arrangement.spacedBy(TopAppBarGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                Box(
                    modifier = Modifier.size(DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center,
                    content = { navigationIcon() },
                )
            }
            DandiText(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongXL,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.defaultMinSize(
                    minWidth = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget,
                    minHeight = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget,
                ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/** Canonical inverse snackbar from Figma node `993:690`. */
@Composable
fun NyummySnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = SnackbarMinimumHeight)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = DesignSystemThemeImpl.designSystemShape.toast,
        color = colors.bgSurfaceInverse,
        contentColor = colors.contentInverseDefault,
    ) {
        Row(
            modifier = Modifier.padding(
                start = DesignSystemThemeImpl.designSystemSpacing.space16,
                top = DesignSystemThemeImpl.designSystemSpacing.space12,
                end = DesignSystemThemeImpl.designSystemSpacing.space12,
                bottom = DesignSystemThemeImpl.designSystemSpacing.space12,
            ),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DandiText(
                text = message,
                modifier = Modifier.weight(1f),
                color = colors.contentInverseDefault,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (actionLabel != null && onAction != null) {
                Box(
                    modifier = Modifier
                        .size(width = SnackbarActionWidth, height = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget)
                        .clickable(role = Role.Button, onClick = onAction),
                    contentAlignment = Alignment.Center,
                ) {
                    DandiText(
                        text = actionLabel,
                        color = colors.contentInverseDefault,
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                    )
                }
            }
        }
    }
}

/** Canonical single-style card from Figma node `993:695`. */
@Composable
fun NyummyCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    body: String? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = CardReferenceHeight)
            .then(interactionModifier),
        shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(ComponentBorderWidth, colors.borderCardSubtle),
    ) {
        if (content != null) {
            Box(
                modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
                contentAlignment = Alignment.TopStart,
                content = { content() },
            )
        } else {
            Column(
                modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
                verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
            ) {
                if (title != null) {
                    DandiText(
                        text = title,
                        color = colors.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.textStrongL,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (body != null) {
                    DandiText(
                        text = body,
                        color = colors.contentDefaultLevel1,
                        style = DesignSystemThemeImpl.typeScale.textRegularM,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Canonical list row from Figma node `993:699`. */
@Composable
fun NyummyListRow(
    modifier: Modifier = Modifier,
    title: String,
    supportingText: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val interactionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ListRowMinimumHeight)
            .then(interactionModifier),
        shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(ComponentBorderWidth, colors.borderMealRow),
    ) {
        Row(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space8),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Box(
                    modifier = Modifier.size(DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center,
                    content = { leading() },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ListRowTextGap),
            ) {
                DandiText(
                    text = title,
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingText != null) {
                    DandiText(
                        text = supportingText,
                        color = colors.contentDefaultLevel1,
                        style = DesignSystemThemeImpl.typeScale.textRegularS,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.size(DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center,
                    content = { trailing() },
                )
            }
        }
    }
}

/** Canonical filter chip from Figma node `993:728`. */
@Composable
fun NyummyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = DesignSystemThemeImpl.designSystemColor
    val container = when {
        !enabled -> colors.bgActionSecondaryDisabled
        selected -> colors.bgSelectionPrimary
        else -> colors.bgActionSecondaryDefault
    }
    val content = when {
        !enabled -> colors.contentActionSecondaryDisabled
        selected -> colors.contentSelectionPrimary
        else -> colors.contentActionSecondary
    }
    val iconContent = when {
        !enabled -> colors.contentActionSecondaryDisabled
        selected -> colors.contentActionPrimary
        else -> colors.contentActionSecondary
    }
    val border = if (enabled && focused) {
        BorderStroke(EmphasisBorderWidth, colors.contentDefaultLevel0)
    } else {
        null
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minWidth = ChipMinimumWidth, minHeight = DesignSystemThemeImpl.designSystemSize.minimumTouchTarget)
            .semantics { this.selected = selected }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = container,
        contentColor = content,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space16),
            horizontalArrangement = Arrangement.spacedBy(
                space = DesignSystemThemeImpl.designSystemSpacing.space8,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(LocalContentColor provides iconContent) {
                    Box(
                        modifier = Modifier.size(ChipIconSize),
                        contentAlignment = Alignment.Center,
                        content = { leadingIcon() },
                    )
                }
            }
            DandiText(
                text = label,
                color = content,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class NyummyBadgeTone {
    Neutral,
    Positive,
    Warning,
    Error,
}

/** Canonical 24 dp badge from Figma node `993:739`. */
@Composable
fun NyummyBadge(
    label: String,
    modifier: Modifier = Modifier,
    tone: NyummyBadgeTone = NyummyBadgeTone.Neutral,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val (container, content) = when (tone) {
        NyummyBadgeTone.Neutral -> colors.bgDefaultLevel2 to colors.contentDefaultLevel1
        NyummyBadgeTone.Positive -> colors.bgSuccessSoft to colors.contentSuccess
        NyummyBadgeTone.Warning -> colors.bgWarningSoft to colors.contentWarning
        NyummyBadgeTone.Error -> colors.bgDangerSoft to colors.contentError
    }
    Surface(
        modifier = modifier.height(BadgeHeight),
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = container,
        contentColor = content,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space8),
            contentAlignment = Alignment.Center,
        ) {
            DandiText(
                text = label,
                color = content,
                style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Canonical determinate progress bar from Figma node `993:753`.
 *
 * @param color 채움 색. 기본은 진행바 기본 토큰이며, 영양소별 색 등 데이터 시맨틱 색이
 * 필요한 화면에서만 지정한다.
 */
@Composable
fun NyummyLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val coercedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = coercedProgress,
        animationSpec = tween(
            durationMillis = ProgressAnimationMillis,
            easing = LinearOutSlowInEasing,
        ),
        label = "NyummyProgress",
    )
    val colors = DesignSystemThemeImpl.designSystemColor
    Canvas(
        modifier = modifier
            .widthIn(min = ProgressReferenceWidth)
            .height(ProgressTrackHeight)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(coercedProgress, 0f..1f)
            },
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = colors.bgProgressTrack,
            cornerRadius = CornerRadius(radius, radius),
        )
        val visualWidth = if (animatedProgress == 0f) {
            ProgressMinimumVisual.toPx()
        } else {
            size.width * animatedProgress
        }
        drawRoundRect(
            color = color ?: colors.dataProgressDefault,
            size = size.copy(width = visualWidth.coerceAtMost(size.width)),
            cornerRadius = CornerRadius(radius, radius),
        )
    }
}

enum class NyummyLoadingSize {
    Small,
    Medium,
    Large,
}

/** Canonical indeterminate indicator from Figma node `993:762`. */
@Composable
fun NyummyLoading(
    modifier: Modifier = Modifier,
    size: NyummyLoadingSize = NyummyLoadingSize.Medium,
    contentDescription: String = "불러오는 중",
) {
    val transition = rememberInfiniteTransition(label = "NyummyLoadingRotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = FullRotation,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LoadingRotationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "NyummyLoadingAngle",
    )
    val metrics = when (size) {
        NyummyLoadingSize.Small -> LoadingMetrics(LoadingSmallSize, LoadingSmallStroke)
        NyummyLoadingSize.Medium -> LoadingMetrics(LoadingMediumSize, LoadingMediumStroke)
        NyummyLoadingSize.Large -> LoadingMetrics(LoadingLargeSize, LoadingLargeStroke)
    }
    val indicatorColor = DesignSystemThemeImpl.designSystemColor.dataProgressDefault
    Canvas(
        modifier = modifier
            .size(metrics.size)
            .graphicsLayer { rotationZ = rotation }
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = LoadingStateDescription
                liveRegion = LiveRegionMode.Polite
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        drawArc(
            color = indicatorColor,
            startAngle = LoadingStartAngle,
            sweepAngle = LoadingSweepAngle,
            useCenter = false,
            style = Stroke(width = metrics.strokeWidth.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * Canonical modal scrim from Figma node `993:764`.
 *
 * Figma's background blur cannot be expressed by Compose's foreground `Modifier.blur` without
 * blurring the wrong layer. The exact scrim token is rendered and the full surface consumes input.
 */
@Composable
fun NyummyModalScrim(
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
) {
    val inputModifier = if (onDismissRequest != null) {
        Modifier.clickable(
            onClickLabel = DismissActionLabel,
            role = Role.Button,
            onClick = onDismissRequest,
        )
    } else {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgScrimModal)
            .then(inputModifier)
            .semantics { contentDescription = ModalScrimDescription },
    )
}

enum class NyummyStateSurfaceType {
    Empty,
    Loading,
    Offline,
    AnalysisFailed,
    PermissionDenied,
    Ended,
    Destructive,
    Partial,
    Retrying,
    RewardPending,
    RewardCompleted,
    AlreadyClaimed,
    ReconcileFailed,
    RateLimited,
}

/** Canonical 190 × 220 state surface matrix from Figma node `475:62`. */
@Composable
fun NyummyStateSurface(
    type: NyummyStateSurfaceType,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val spec = stateSurfaceSpec(type)
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier
            .requiredSize(width = StateSurfaceWidth, height = StateSurfaceHeight)
            .semantics {
                stateDescription = spec.title
                liveRegion = LiveRegionMode.Polite
            },
        shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(ComponentBorderWidth, colors.borderCardSubtle),
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .offset(x = StateHorizontalInset, y = StatePillTop)
                    .size(width = StateInnerWidth, height = StatePillHeight),
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                color = spec.pillBackground,
                contentColor = spec.pillContent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DandiText(
                        text = spec.pillLabel,
                        color = spec.pillContent,
                        textAlign = TextAlign.Center,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = StateDecorativeLeft, y = StateDecorativeTop)
                    .size(StateDecorativeSize)
                    .background(spec.decorativeBackground, DesignSystemThemeImpl.designSystemShape.pill)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(StateDecorativeDotSize)
                        .background(spec.decorativeDot, DesignSystemThemeImpl.designSystemShape.pill),
                )
            }
            DandiText(
                text = spec.title,
                modifier = Modifier
                    .offset(x = StateTextLeft, y = StateTitleTop)
                    .width(StateTextWidth)
                    .semantics { heading() },
                color = colors.contentDefaultLevel0,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
            DandiText(
                text = spec.supportingText,
                modifier = Modifier
                    .offset(x = StateTextLeft, y = StateSupportingTop)
                    .width(StateTextWidth),
                color = colors.contentStatusSupporting,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
            if (type == NyummyStateSurfaceType.Destructive) {
                StateSurfaceAction(
                    label = spec.secondaryActionLabel.orEmpty(),
                    background = colors.bgActionSecondaryDefault,
                    content = colors.contentActionSecondary,
                    modifier = Modifier
                        .offset(x = StateHorizontalInset, y = StateActionTouchTop)
                        .size(width = StateDestructiveActionWidth, height = StateActionTouchHeight),
                    onClick = onSecondaryAction,
                )
                StateSurfaceAction(
                    label = spec.actionLabel,
                    background = spec.actionBackground,
                    content = spec.actionContent,
                    modifier = Modifier
                        .offset(x = StateDestructiveSecondLeft, y = StateActionTouchTop)
                        .size(width = StateDestructiveActionWidth, height = StateActionTouchHeight),
                    onClick = onAction,
                )
            } else {
                StateSurfaceAction(
                    label = spec.actionLabel,
                    background = spec.actionBackground,
                    content = spec.actionContent,
                    modifier = Modifier
                        .offset(x = StateHorizontalInset, y = StateActionTouchTop)
                        .size(width = StateInnerWidth, height = StateActionTouchHeight),
                    onClick = if (spec.actionIsStatus) null else onAction,
                    status = spec.actionIsStatus,
                )
            }
        }
    }
}

@Composable
private fun StateSurfaceAction(
    label: String,
    background: Color,
    content: Color,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    status: Boolean = false,
) {
    val interactionModifier = when {
        onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)
        status -> Modifier.semantics {
            stateDescription = label
            liveRegion = LiveRegionMode.Polite
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        }
        else -> Modifier
    }
    Box(
        modifier = modifier.then(interactionModifier),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(StateActionVisualHeight),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(StateActionRadius),
            color = background,
            contentColor = content,
        ) {
            Box(contentAlignment = Alignment.Center) {
                DandiText(
                    text = label,
                    color = content,
                    textAlign = TextAlign.Center,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
            }
        }
    }
}

@Composable
private fun stateSurfaceSpec(type: NyummyStateSurfaceType): StateSurfaceSpec {
    val colors = DesignSystemThemeImpl.designSystemColor
    return when (type) {
        NyummyStateSurfaceType.Empty -> StateSurfaceSpec(
            pillLabel = "Empty",
            title = "아직 기록이 없어요",
            supportingText = "첫 식사를 남겨 볼까요?",
            pillBackground = colors.bgSuccessSoft,
            pillContent = colors.contentSuccess,
            decorativeBackground = colors.bgSuccessSoft,
            decorativeDot = colors.contentSuccess,
            actionLabel = "기록 시작",
            actionBackground = colors.bgActionSecondaryDefault,
            actionContent = colors.contentActionSecondary,
        )

        NyummyStateSurfaceType.Loading -> StateSurfaceSpec.processing(
            pillLabel = "Loading",
            title = "기록을 불러오는 중",
            supportingText = "잠시만 기다려 주세요",
            actionLabel = "진행 표시",
            colors = colors,
        )

        NyummyStateSurfaceType.Offline -> StateSurfaceSpec.warning(
            pillLabel = "Offline",
            title = "연결을 확인해 주세요",
            supportingText = "저장된 내용은 그대로예요",
            actionLabel = "다시 시도",
            colors = colors,
        )

        NyummyStateSurfaceType.AnalysisFailed -> StateSurfaceSpec.error(
            pillLabel = "Analysis Failed",
            title = "분석하지 못했어요",
            supportingText = "사진과 텍스트는 보존했어요",
            actionLabel = "다시 분석",
            colors = colors,
        )

        NyummyStateSurfaceType.PermissionDenied -> StateSurfaceSpec.warning(
            pillLabel = "Permission Denied",
            title = "사진 접근이 필요해요",
            supportingText = "기록하려면 권한이 필요해요",
            actionLabel = "설정 열기",
            colors = colors,
        )

        NyummyStateSurfaceType.Ended -> StateSurfaceSpec.neutral(
            pillLabel = "Ended",
            title = "이벤트가 끝났어요",
            supportingText = "새로운 소식을 확인해 주세요",
            actionLabel = "다른 소식",
            colors = colors,
        )

        NyummyStateSurfaceType.Destructive -> StateSurfaceSpec.error(
            pillLabel = "Destructive",
            title = "기록을 삭제할까요?",
            supportingText = "삭제하면 되돌릴 수 없어요",
            actionLabel = "삭제",
            colors = colors,
        ).copy(secondaryActionLabel = "취소")

        NyummyStateSurfaceType.Partial -> StateSurfaceSpec(
            pillLabel = "Partial",
            title = "일부만 분석됐어요",
            supportingText = "완료된 식사 기준으로 보여요",
            pillBackground = colors.bgWarningSoft,
            pillContent = colors.contentError,
            decorativeBackground = colors.bgWarningSoft,
            decorativeDot = colors.contentIconWarning,
            actionLabel = "실패 식사 보기",
            actionBackground = colors.bgWarningSoft,
            actionContent = colors.contentActionDanger,
        )

        NyummyStateSurfaceType.Retrying -> StateSurfaceSpec.processing(
            pillLabel = "Retrying",
            title = "다시 분석하는 중",
            supportingText = "기존 기록은 그대로 보존해요",
            actionLabel = "진행 표시",
            colors = colors,
        )

        NyummyStateSurfaceType.RewardPending -> StateSurfaceSpec.processing(
            pillLabel = "Reward Pending",
            title = "보상 처리 중",
            supportingText = "중복 없이 안전하게 확인해요",
            actionLabel = "처리 중",
            colors = colors,
        )

        NyummyStateSurfaceType.RewardCompleted -> StateSurfaceSpec(
            pillLabel = "Reward Completed",
            title = "코인 80개 받았어요",
            supportingText = "지갑에 바로 반영했어요",
            pillBackground = colors.bgSuccessSoft,
            pillContent = colors.contentSuccess,
            decorativeBackground = colors.bgSuccessSoft,
            decorativeDot = colors.contentSuccess,
            actionLabel = "확인",
            actionBackground = colors.bgActionSecondaryDefault,
            actionContent = colors.contentActionSecondary,
        )

        NyummyStateSurfaceType.AlreadyClaimed -> StateSurfaceSpec.neutral(
            pillLabel = "Already Claimed",
            title = "이미 받은 보상이에요",
            supportingText = "현재 지갑 잔액을 확인해요",
            actionLabel = "지갑 보기",
            colors = colors,
        )

        NyummyStateSurfaceType.ReconcileFailed -> StateSurfaceSpec.error(
            pillLabel = "Reconcile Failed",
            title = "보상 확인이 필요해요",
            supportingText = "지급 결과는 안전하게 보존돼요",
            actionLabel = "다시 확인",
            colors = colors,
        )

        NyummyStateSurfaceType.RateLimited -> StateSurfaceSpec.warning(
            pillLabel = "Rate Limited",
            title = "잠시 후 다시 해주세요",
            supportingText = "요청이 많아 잠시 쉬어가요",
            actionLabel = "10초 후 다시 시도",
            colors = colors,
        )
    }
}

private data class StateSurfaceSpec(
    val pillLabel: String,
    val title: String,
    val supportingText: String,
    val pillBackground: Color,
    val pillContent: Color,
    val decorativeBackground: Color,
    val decorativeDot: Color,
    val actionLabel: String,
    val actionBackground: Color,
    val actionContent: Color,
    val secondaryActionLabel: String? = null,
    val actionIsStatus: Boolean = false,
) {
    companion object {
        fun processing(
            pillLabel: String,
            title: String,
            supportingText: String,
            actionLabel: String,
            colors: com.dandi.nyummy.common.presentation.ui.color.DesignSystemSemanticColors,
        ) = StateSurfaceSpec(
            pillLabel = pillLabel,
            title = title,
            supportingText = supportingText,
            pillBackground = colors.bgStatusProcessing,
            pillContent = colors.contentStatusProcessing,
            decorativeBackground = colors.bgStatusProcessing,
            decorativeDot = colors.contentStatusProcessing,
            actionLabel = actionLabel,
            actionBackground = colors.bgStatusProcessing,
            actionContent = colors.contentStatusProcessing,
            actionIsStatus = true,
        )

        fun warning(
            pillLabel: String,
            title: String,
            supportingText: String,
            actionLabel: String,
            colors: com.dandi.nyummy.common.presentation.ui.color.DesignSystemSemanticColors,
        ) = StateSurfaceSpec(
            pillLabel = pillLabel,
            title = title,
            supportingText = supportingText,
            pillBackground = colors.bgWarningSoft,
            pillContent = colors.contentStatusWarning,
            decorativeBackground = colors.bgWarningSoft,
            decorativeDot = colors.contentStatusWarning,
            actionLabel = actionLabel,
            actionBackground = colors.bgWarningSoft,
            actionContent = colors.contentWarning,
        )

        fun error(
            pillLabel: String,
            title: String,
            supportingText: String,
            actionLabel: String,
            colors: com.dandi.nyummy.common.presentation.ui.color.DesignSystemSemanticColors,
        ) = StateSurfaceSpec(
            pillLabel = pillLabel,
            title = title,
            supportingText = supportingText,
            pillBackground = colors.bgDangerSoft,
            pillContent = colors.contentError,
            decorativeBackground = colors.bgDangerSoft,
            decorativeDot = colors.contentError,
            actionLabel = actionLabel,
            actionBackground = colors.bgActionDangerDefault,
            actionContent = colors.contentActionDanger,
        )

        fun neutral(
            pillLabel: String,
            title: String,
            supportingText: String,
            actionLabel: String,
            colors: com.dandi.nyummy.common.presentation.ui.color.DesignSystemSemanticColors,
        ) = StateSurfaceSpec(
            pillLabel = pillLabel,
            title = title,
            supportingText = supportingText,
            pillBackground = colors.bgSurfaceStrong,
            pillContent = colors.contentStatusSupporting,
            decorativeBackground = colors.bgSurfaceStrong,
            decorativeDot = colors.contentStatusSupporting,
            actionLabel = actionLabel,
            actionBackground = colors.bgActionSecondaryDisabled,
            actionContent = colors.contentStatusSupporting,
        )
    }
}

private data class LoadingMetrics(val size: Dp, val strokeWidth: Dp)

private const val ProgressAnimationMillis = 600
private const val LoadingRotationMillis = 900
private const val FullRotation = 360f
private const val LoadingStartAngle = -90f
private const val LoadingSweepAngle = 270f
private const val LoadingStateDescription = "진행 중"
private const val DismissActionLabel = "닫기"
private const val ModalScrimDescription = "모달 배경"
private val TopAppBarHeight = 64.dp
private val TopAppBarPadding = 4.dp
private val TopAppBarGap = 4.dp
private val SnackbarMinimumHeight = 64.dp
private val SnackbarActionWidth = 96.dp
private val CardReferenceHeight = 132.dp
private val ListRowMinimumHeight = 64.dp
private val ListRowTextGap = 2.dp
private val ComponentBorderWidth = 1.dp
private val EmphasisBorderWidth = 2.dp
private val ChipMinimumWidth = 72.dp
private val ChipIconSize = 18.dp
private val BadgeHeight = 24.dp
private val ProgressReferenceWidth = 320.dp
private val ProgressTrackHeight = 8.dp
private val ProgressMinimumVisual = 1.dp
private val LoadingSmallSize = 16.dp
private val LoadingMediumSize = 24.dp
private val LoadingLargeSize = 32.dp
private val LoadingSmallStroke = 2.dp
private val LoadingMediumStroke = 3.dp
private val LoadingLargeStroke = 3.dp
private val StateSurfaceWidth = 190.dp
private val StateSurfaceHeight = 220.dp
private val StateHorizontalInset = 14.dp
private val StateInnerWidth = 162.dp
private val StatePillTop = 14.dp
private val StatePillHeight = 30.dp
private val StateDecorativeLeft = 73.dp
private val StateDecorativeTop = 58.dp
private val StateDecorativeSize = 44.dp
private val StateDecorativeDotSize = 12.dp
private val StateTextLeft = 12.dp
private val StateTextWidth = 166.dp
private val StateTitleTop = 112.dp
private val StateSupportingTop = 140.dp
private val StateActionTouchTop = 162.dp
private val StateActionTouchHeight = 48.dp
private val StateActionVisualHeight = 44.dp
private val StateActionRadius = 14.dp
private val StateDestructiveActionWidth = 78.dp
private val StateDestructiveSecondLeft = 98.dp

@Preview(showBackground = true, widthDp = 440, heightDp = 1660)
@Composable
private fun NyummyStateSurfaceMatrixPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12),
        ) {
            NyummyStateSurfaceType.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    row.forEach { type ->
                        NyummyStateSurface(type = type)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 432, heightDp = 480)
@Composable
private fun NyummySurfaceMatrixPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12),
        ) {
            NyummyTopAppBar(title = "식사 기록")
            NyummySnackbar(
                message = "저장하지 못했어요. 다시 시도해 주세요.",
                actionLabel = "다시 시도",
                onAction = {},
            )
            NyummyCard(
                title = "하루 영양 현황",
                body = "목표와 현재 섭취량을 한눈에 확인할 수 있어요.",
                modifier = Modifier.fillMaxWidth(),
            )
            NyummyListRow(
                title = "닭가슴살 포케",
                supportingText = "오후 12:36 · 540 kcal",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                NyummyChip(label = "전체", selected = false, onClick = {})
                NyummyChip(label = "선택", selected = true, onClick = {})
                NyummyBadge(label = "완료", tone = NyummyBadgeTone.Positive)
            }
            NyummyLinearProgress(progress = 0.5f, modifier = Modifier.fillMaxWidth())
        }
    }
}
