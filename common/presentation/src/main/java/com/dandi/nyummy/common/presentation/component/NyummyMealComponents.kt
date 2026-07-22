package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow

enum class NyummyAnalysisState {
    Completed,
    Analyzing,
    Failed,
    Retrying,
}

enum class NyummyCalendarNutritionStatus {
    Positive,
    OutOfRange,
    NoRecord,
    None,
}

enum class NyummyCalendarWeekday {
    Weekday,
    Saturday,
    Sunday,
}

@Composable
fun NyummyCalendarDay(
    day: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    selected: Boolean = false,
    inCurrentMonth: Boolean = true,
    weekday: NyummyCalendarWeekday = NyummyCalendarWeekday.Weekday,
    nutritionStatus: NyummyCalendarNutritionStatus = NyummyCalendarNutritionStatus.None,
    firstFoodIcon: (@Composable () -> Unit)? = null,
    secondFoodIcon: (@Composable () -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val dateColor = when {
        !inCurrentMonth -> colors.contentCalendarAdjacentMonth
        weekday == NyummyCalendarWeekday.Saturday -> colors.contentCalendarSaturday
        weekday == NyummyCalendarWeekday.Sunday -> colors.contentCalendarSunday
        else -> colors.contentCalendarDate
    }
    val iconCount = when {
        firstFoodIcon == null -> 0
        secondFoodIcon == null -> 1
        else -> 2
    }
    val nutritionDescription = when (nutritionStatus) {
        NyummyCalendarNutritionStatus.Positive -> "영양 균형 양호"
        NyummyCalendarNutritionStatus.OutOfRange -> "영양 범위 초과"
        NyummyCalendarNutritionStatus.NoRecord -> "영양 기록 없음"
        NyummyCalendarNutritionStatus.None -> null
    }
    val descriptions = listOfNotNull(
        "선택됨".takeIf { selected },
        nutritionDescription,
        "식사 ${iconCount}개".takeIf { iconCount > 0 },
    ).joinToString()
    val selectedBorder = if (selected) {
        Modifier.border(CalendarBorderWidth, colors.contentActionSecondary, RectangleShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(CalendarDayWidth, CalendarDayHeight)
            .then(selectedBorder)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Button,
            )
            .semantics {
                this.selected = selected
                if (descriptions.isNotEmpty()) stateDescription = descriptions
            },
    ) {
        DandiText(
            text = day,
            modifier = Modifier
                .offset(y = CalendarDateOffsetY)
                .size(CalendarDayWidth, CalendarDateHeight),
            color = dateColor,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Clip,
        )
        when (iconCount) {
            1 -> CalendarFoodIcon(
                modifier = Modifier.offset(x = CalendarSingleIconOffsetX, y = CalendarFoodIconOffsetY),
                content = requireNotNull(firstFoodIcon),
            )

            2 -> {
                CalendarFoodIcon(
                    modifier = Modifier.offset(x = CalendarFirstDoubleIconOffsetX, y = CalendarFoodIconOffsetY),
                    content = requireNotNull(firstFoodIcon),
                )
                CalendarFoodIcon(
                    modifier = Modifier.offset(x = CalendarSecondDoubleIconOffsetX, y = CalendarFoodIconOffsetY),
                    content = requireNotNull(secondFoodIcon),
                )
            }
        }
        CalendarNutritionMarker(
            status = nutritionStatus,
            modifier = Modifier.offset(x = CalendarMarkerOffsetX, y = CalendarMarkerOffsetY),
        )
    }
}

@Composable
private fun CalendarFoodIcon(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(DesignSystemThemeImpl.designSystemSize.calendarFoodIcon),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun CalendarNutritionMarker(
    status: NyummyCalendarNutritionStatus,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    if (status == NyummyCalendarNutritionStatus.None) return

    Canvas(modifier.size(DesignSystemThemeImpl.designSystemSize.calendarStatusMarker)) {
        when (status) {
            NyummyCalendarNutritionStatus.Positive -> {
                drawCircle(colors.dataEvaluationPositive)
                drawLine(
                    color = colors.contentInverseDefault,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.23f, size.height * 0.52f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.72f),
                    strokeWidth = PositiveCheckStroke.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.contentInverseDefault,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.72f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.28f),
                    strokeWidth = PositiveCheckStroke.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            NyummyCalendarNutritionStatus.OutOfRange -> drawCircle(colors.dataEvaluationNegative)
            NyummyCalendarNutritionStatus.NoRecord -> drawCircle(
                color = colors.dataEvaluationUnrecorded,
                style = Stroke(width = CalendarMarkerBorderWidth.toPx()),
            )

            NyummyCalendarNutritionStatus.None -> Unit
        }
    }
}

enum class NyummyCalendarHeaderDirection {
    Previous,
    Next,
}

@Composable
fun NyummyCalendarHeaderAction(
    direction: NyummyCalendarHeaderDirection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val directionDescription = when (direction) {
        NyummyCalendarHeaderDirection.Previous -> "이전 달"
        NyummyCalendarHeaderDirection.Next -> "다음 달"
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(CalendarHeaderActionSize)
            .semantics { contentDescription = directionDescription },
        enabled = enabled,
        shape = RoundedCornerShape(CalendarHeaderActionRadius),
        color = colors.bgSurfaceControl,
        contentColor = colors.contentCalendarAction,
        border = BorderStroke(CalendarBorderWidth, colors.borderCalendarOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(CalendarChevronSize)) {
                val startX = if (direction == NyummyCalendarHeaderDirection.Previous) {
                    size.width * 0.625f
                } else {
                    size.width * 0.375f
                }
                val endX = if (direction == NyummyCalendarHeaderDirection.Previous) {
                    size.width * 0.375f
                } else {
                    size.width * 0.625f
                }
                drawLine(
                    color = colors.contentCalendarAction,
                    start = androidx.compose.ui.geometry.Offset(startX, size.height * 0.25f),
                    end = androidx.compose.ui.geometry.Offset(endX, size.height * 0.5f),
                    strokeWidth = CalendarChevronStroke.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = colors.contentCalendarAction,
                    start = androidx.compose.ui.geometry.Offset(endX, size.height * 0.5f),
                    end = androidx.compose.ui.geometry.Offset(startX, size.height * 0.75f),
                    strokeWidth = CalendarChevronStroke.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Immutable
data class NyummyMealRowData(
    val orderLabel: String,
    val name: String,
    val recordedMeta: String,
    val calories: String,
)

@Composable
fun NyummyMealRow(
    data: NyummyMealRowData,
    modifier: Modifier = Modifier,
    state: NyummyAnalysisState = NyummyAnalysisState.Completed,
    onClick: () -> Unit,
    onRetry: () -> Unit = {},
    foodIcon: (@Composable () -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val isCompleted = state == NyummyAnalysisState.Completed
    val contentWidth = if (isCompleted) MealRowCompletedContentWidth else MealRowStateContentWidth
    val trailingWidth = if (isCompleted) MealRowCompletedTrailingWidth else MealRowStateTrailingWidth
    val borderColor = if (isCompleted) colors.borderMealRow else colors.borderStatusProcessing
    val borderWidth = if (isCompleted) MealRowBorderWidth else MealRowStateBorderWidth

    Surface(
        onClick = onClick,
        modifier = modifier.size(MealRowWidth, DesignSystemThemeImpl.designSystemSize.mealRowMinHeight),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignSystemThemeImpl.designSystemSpacing.space12),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(DesignSystemThemeImpl.designSystemSize.mealLeadingIcon),
                contentAlignment = Alignment.Center,
            ) {
                if (foodIcon == null) {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.nyummy_food_salad),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        filterQuality = FilterQuality.None,
                    )
                } else {
                    foodIcon()
                }
            }
            Column(
                modifier = Modifier.size(contentWidth, MealRowInnerHeight),
                verticalArrangement = Arrangement.Center,
            ) {
                DandiText(
                    text = data.orderLabel,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.contentCalendarAction,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    overflow = TextOverflow.Ellipsis,
                )
                DandiText(
                    text = data.name,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCompleted) {
                Column(
                    modifier = Modifier.size(trailingWidth, MealRowInnerHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.End,
                ) {
                    DandiText(
                        text = data.recordedMeta,
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.contentDefaultLevel1,
                        style = DesignSystemThemeImpl.typeScale.textRegularS,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                    )
                    DandiText(
                        text = data.calories,
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                        textAlign = TextAlign.End,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                MealAnalysisStateAction(
                    state = state,
                    modifier = Modifier.size(trailingWidth, MealRowInnerHeight),
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun MealAnalysisStateAction(
    state: NyummyAnalysisState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val failed = state == NyummyAnalysisState.Failed
    val label = when (state) {
        NyummyAnalysisState.Analyzing -> "분석 중…"
        NyummyAnalysisState.Failed -> "다시 시도"
        NyummyAnalysisState.Retrying -> "다시 분석 중…"
        NyummyAnalysisState.Completed -> return
    }
    Surface(
        onClick = onRetry,
        modifier = modifier,
        enabled = failed,
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = if (failed) colors.bgDangerSoft else colors.bgStatusProcessing,
        contentColor = if (failed) colors.contentError else colors.contentStatusProcessing,
        border = BorderStroke(MealRowBorderWidth, colors.borderStatusProcessing),
    ) {
        Box(contentAlignment = Alignment.Center) {
            DandiText(
                text = label,
                color = if (failed) colors.contentError else colors.contentStatusProcessing,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

enum class NyummyAnalysisBannerState {
    Failed,
    Retrying,
}

@Composable
fun NyummyAnalysisBanner(
    title: String,
    modifier: Modifier = Modifier,
    message: String,
    state: NyummyAnalysisBannerState,
    onRetry: () -> Unit = {},
    guideCharacter: @Composable () -> Unit = {},
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val isFailed = state == NyummyAnalysisBannerState.Failed

    Surface(
        modifier = modifier.size(AnalysisBannerWidth, AnalysisBannerHeight),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
        color = colors.bgStatusProcessing,
        contentColor = colors.contentDefaultLevel1,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AnalysisBannerHorizontalPadding,
                vertical = DesignSystemThemeImpl.designSystemSpacing.space8,
            ),
            horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(AnalysisGuideWidth, AnalysisGuideHeight),
                contentAlignment = Alignment.Center,
            ) {
                guideCharacter()
            }
            Column(
                modifier = Modifier.size(
                    width = if (isFailed) AnalysisFailedMessageWidth else AnalysisRetryingMessageWidth,
                    height = AnalysisBannerMessageHeight,
                ),
                verticalArrangement = Arrangement.Center,
            ) {
                DandiText(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isFailed) colors.contentError else colors.contentStatusProcessing,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    overflow = TextOverflow.Ellipsis,
                )
                DandiText(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.contentDefaultLevel1,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isFailed) {
                Surface(
                    onClick = onRetry,
                    modifier = Modifier.size(AnalysisRetryWidth, DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
                    shape = DesignSystemThemeImpl.designSystemShape.pill,
                    color = colors.bgActionDangerDefault,
                    contentColor = colors.contentActionDanger,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        DandiText(
                            text = "다시 분석",
                            color = colors.contentActionDanger,
                            style = DesignSystemThemeImpl.typeScale.labelStrongS,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Immutable
data class NyummyDailyMacroSummary(
    val label: String,
    val percentageLabel: String,
    val progress: Float,
)

@Immutable
data class NyummyDailyNutritionData(
    val currentCalories: String,
    val targetCalories: String,
    val calorieRatioLabel: String,
    val carbohydrate: NyummyDailyMacroSummary,
    val protein: NyummyDailyMacroSummary,
    val fat: NyummyDailyMacroSummary,
)

enum class NyummyDailyNutritionState {
    Default,
    Loading,
    Partial,
    Failed,
    Retrying,
}

@Composable
fun NyummyDailyNutritionSummary(
    data: NyummyDailyNutritionData,
    modifier: Modifier = Modifier,
    state: NyummyDailyNutritionState = NyummyDailyNutritionState.Default,
    stateNote: String = state.defaultNote,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val loading = state == NyummyDailyNutritionState.Loading
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20)
    val noteColor = when (state) {
        NyummyDailyNutritionState.Default -> colors.contentNutritionLabel
        NyummyDailyNutritionState.Loading,
        NyummyDailyNutritionState.Retrying,
        -> colors.contentStatusProcessing

        NyummyDailyNutritionState.Partial -> colors.contentStatusWarning
        NyummyDailyNutritionState.Failed -> colors.contentError
    }

    Surface(
        modifier = modifier
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
            )
            .size(DailySummaryWidth, DailySummaryHeight),
        shape = shape,
        color = colors.bgSurfaceCardSubtle,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(DailySummaryBorderWidth, colors.borderNutrition),
    ) {
        Box(Modifier.fillMaxSize()) {
            DandiText(
                text = "하루 영양 현황",
                modifier = Modifier
                    .offset(x = DailySummaryHorizontalInset, y = DailySummaryTitleOffsetY)
                    .size(DailySummaryTitleWidth, DailySummaryTitleHeight),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
                overflow = TextOverflow.Clip,
            )
            Box(
                modifier = Modifier
                    .offset(x = DailySummaryNoteOffsetX, y = DailySummaryTitleOffsetY)
                    .size(DailySummaryNoteWidth, DailySummaryTitleHeight),
                contentAlignment = Alignment.CenterEnd,
            ) {
                DandiText(
                    text = stateNote,
                    modifier = Modifier.fillMaxWidth(),
                    color = noteColor,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DandiText(
                text = if (loading) "— / ${data.targetCalories} kcal" else "${data.currentCalories} / ${data.targetCalories} kcal",
                modifier = Modifier
                    .offset(x = DailySummaryHorizontalInset, y = DailySummaryCalorieOffsetY)
                    .size(DailySummaryCalorieWidth, DailySummaryTitleHeight),
                color = if (loading) colors.contentDefaultLevel1 else colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
                overflow = TextOverflow.Clip,
            )
            Surface(
                modifier = Modifier
                    .offset(x = DailySummaryRatioOffsetX, y = DailySummaryRatioOffsetY)
                    .size(DailySummaryRatioWidth, DailySummaryRatioHeight),
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                color = colors.bgStatusWarning,
                contentColor = if (loading) colors.contentStatusProcessing else colors.contentStatusWarning,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DandiText(
                        text = if (loading) "—" else data.calorieRatioLabel,
                        color = if (loading) colors.contentStatusProcessing else colors.contentStatusWarning,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            DailyMacro(
                macro = data.carbohydrate,
                loading = loading,
                color = colors.dataNutrientCarbohydrate,
                modifier = Modifier.offset(x = DailyMacroFirstOffsetX, y = DailyMacroOffsetY),
            )
            DailyMacro(
                macro = data.protein,
                loading = loading,
                color = colors.dataNutrientProtein,
                modifier = Modifier.offset(x = DailyMacroSecondOffsetX, y = DailyMacroOffsetY),
            )
            DailyMacro(
                macro = data.fat,
                loading = loading,
                color = colors.dataNutrientFat,
                modifier = Modifier.offset(x = DailyMacroThirdOffsetX, y = DailyMacroOffsetY),
            )
        }
    }
}

private val NyummyDailyNutritionState.defaultNote: String
    get() = when (this) {
        NyummyDailyNutritionState.Default -> "5끼 반영 완료"
        NyummyDailyNutritionState.Loading -> "영양 정보를 불러오는 중"
        NyummyDailyNutritionState.Partial -> "4끼 반영 · 1끼 분석 대기"
        NyummyDailyNutritionState.Failed -> "4끼 반영 · 1끼 분석 실패"
        NyummyDailyNutritionState.Retrying -> "4끼 반영 · 1끼 다시 분석 중"
    }

@Composable
private fun DailyMacro(
    macro: NyummyDailyMacroSummary,
    loading: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier.size(DailyMacroWidth, DailyMacroHeight)) {
        DandiText(
            text = if (loading) "${macro.label} —" else "${macro.label} ${macro.percentageLabel}",
            modifier = Modifier.size(DailyMacroWidth, DailyMacroLabelHeight),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .offset(y = DailyMacroTrackOffsetY)
                .size(DailyMacroWidth, DailyMacroTrackHeight)
                .background(colors.bgProgressTrack, DesignSystemThemeImpl.designSystemShape.pill),
        ) {
            if (!loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(macro.progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color, DesignSystemThemeImpl.designSystemShape.pill),
                )
            }
        }
    }
}

@Immutable
data class NyummyMealDetailData(
    val orderAndTime: String,
    val name: String,
    val calories: String,
    val capturedAt: String,
)

@Composable
fun NyummyMealDetailCard(
    data: NyummyMealDetailData,
    modifier: Modifier = Modifier,
    state: NyummyAnalysisState = NyummyAnalysisState.Completed,
    onEditText: () -> Unit = {},
    onAnalyzeAgain: () -> Unit = {},
    onDelete: () -> Unit = {},
    photo: (@Composable () -> Unit)? = null,
    foodIcon: (@Composable () -> Unit)? = null,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16)

    Surface(
        modifier = modifier.size(MealDetailWidth, MealDetailHeight),
        shape = shape,
        color = colors.bgSurfaceIvory,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(MealDetailBorderWidth, colors.borderCardSubtle),
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .offset(MealDetailInset, MealDetailInset)
                    .size(MealDetailPhotoSize),
                shape = shape,
                color = colors.bgMealPhoto,
                border = BorderStroke(MealDetailBorderWidth, colors.borderMealPhoto),
            ) {
                if (photo == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            modifier = Modifier
                                .offset(y = MealDetailRequiredPillOffsetY)
                                .size(MealDetailRequiredPillWidth, MealDetailStatusHeight),
                            shape = DesignSystemThemeImpl.designSystemShape.pill,
                            color = colors.bgDefaultLevel1,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                DandiText(
                                    text = "필수 사진",
                                    color = colors.contentNutritionLabel,
                                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        DandiText(
                            text = "사용자가 촬영한 원본",
                            modifier = Modifier
                                .offset(y = MealDetailPhotoLabelOffsetY)
                                .width(MealDetailPhotoLabelWidth),
                            color = colors.contentNutritionLabel,
                            style = DesignSystemThemeImpl.typeScale.labelStrongS,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { photo() }
                }
            }
            Box(
                modifier = Modifier
                    .offset(x = MealDetailContentOffsetX, y = MealDetailFoodIconOffsetY)
                    .size(DesignSystemThemeImpl.designSystemSize.mealLeadingIcon),
                contentAlignment = Alignment.Center,
            ) {
                if (foodIcon == null) {
                    Image(
                        bitmap = ImageBitmap.imageResource(R.drawable.nyummy_food_salad),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        filterQuality = FilterQuality.None,
                    )
                } else {
                    foodIcon()
                }
            }
            DandiText(
                text = data.orderAndTime,
                modifier = Modifier
                    .offset(x = MealDetailOrderOffsetX, y = MealDetailOrderOffsetY)
                    .size(MealDetailOrderWidth, MealDetailStatusTextHeight),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                overflow = TextOverflow.Ellipsis,
            )
            DandiText(
                text = data.name,
                modifier = Modifier
                    .offset(x = MealDetailContentOffsetX, y = MealDetailNameOffsetY)
                    .size(MealDetailNameWidth, MealDetailNameHeight),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularM,
                overflow = TextOverflow.Ellipsis,
            )
            DandiText(
                text = data.calories,
                modifier = Modifier
                    .offset(x = MealDetailContentOffsetX, y = MealDetailCaloriesOffsetY)
                    .size(MealDetailCaloriesWidth, MealDetailCaloriesHeight),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.numberStrongL,
                overflow = TextOverflow.Clip,
            )
            DandiText(
                text = "kcal",
                modifier = Modifier
                    .offset(x = MealDetailCalorieUnitOffsetX, y = MealDetailCalorieUnitOffsetY)
                    .size(MealDetailCalorieUnitWidth, MealDetailStatusTextHeight),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
            Surface(
                modifier = Modifier
                    .offset(x = MealDetailContentOffsetX, y = MealDetailCapturePillOffsetY)
                    .size(
                        width = if (state == NyummyAnalysisState.Failed) {
                            MealDetailFailedCapturePillWidth
                        } else {
                            MealDetailCapturePillWidth
                        },
                        height = MealDetailStatusHeight,
                    ),
                shape = DesignSystemThemeImpl.designSystemShape.pill,
                color = colors.bgStatusProcessing,
                contentColor = colors.contentNutritionLabel,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DandiText(
                        text = "촬영 시각 · 수정 불가",
                        color = colors.contentNutritionLabel,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            DandiText(
                text = "촬영 시각 · ${data.capturedAt}",
                modifier = Modifier
                    .offset(x = MealDetailContentOffsetX, y = MealDetailTimestampOffsetY)
                    .size(MealDetailTimestampWidth, MealDetailStatusTextHeight),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                overflow = TextOverflow.Ellipsis,
            )
            MealDetailStatus(
                state = state,
                modifier = Modifier.offset(x = MealDetailStatusOffsetX, y = MealDetailInset),
            )
            MealDetailAction(
                label = "텍스트 수정",
                modifier = Modifier.offset(x = MealDetailFirstActionOffsetX, y = MealDetailActionsOffsetY),
                onClick = onEditText,
                kind = MealDetailActionKind.Secondary,
            )
            MealDetailAction(
                label = when (state) {
                    NyummyAnalysisState.Analyzing -> "분석 중…"
                    NyummyAnalysisState.Retrying -> "다시 분석 중…"
                    NyummyAnalysisState.Completed,
                    NyummyAnalysisState.Failed,
                    -> "다시 분석"
                },
                modifier = Modifier.offset(x = MealDetailSecondActionOffsetX, y = MealDetailActionsOffsetY),
                onClick = onAnalyzeAgain,
                kind = if (state == NyummyAnalysisState.Analyzing || state == NyummyAnalysisState.Retrying) {
                    MealDetailActionKind.Disabled
                } else {
                    MealDetailActionKind.SecondaryBorderless
                },
            )
            MealDetailAction(
                label = "삭제",
                modifier = Modifier.offset(x = MealDetailThirdActionOffsetX, y = MealDetailActionsOffsetY),
                onClick = onDelete,
                kind = MealDetailActionKind.Danger,
            )
        }
    }
}

@Composable
private fun MealDetailStatus(
    state: NyummyAnalysisState,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val label = when (state) {
        NyummyAnalysisState.Completed -> "분석 완료"
        NyummyAnalysisState.Analyzing -> "분석 중"
        NyummyAnalysisState.Failed -> "분석 실패"
        NyummyAnalysisState.Retrying -> "다시 분석 중"
    }
    val warning = state == NyummyAnalysisState.Failed
    Surface(
        modifier = modifier.size(MealDetailStatusWidth, MealDetailStatusHeight),
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = if (warning) colors.bgStatusWarning else colors.bgStatusProcessing,
        contentColor = if (warning) colors.contentStatusWarning else colors.contentStatusProcessing,
    ) {
        Box(contentAlignment = Alignment.Center) {
            DandiText(
                text = label,
                color = if (warning) colors.contentStatusWarning else colors.contentStatusProcessing,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private enum class MealDetailActionKind {
    Secondary,
    SecondaryBorderless,
    Disabled,
    Danger,
}

@Composable
private fun MealDetailAction(
    label: String,
    onClick: () -> Unit,
    kind: MealDetailActionKind,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val enabled = kind != MealDetailActionKind.Disabled
    val container = when (kind) {
        MealDetailActionKind.Secondary,
        MealDetailActionKind.SecondaryBorderless,
        -> colors.bgActionSecondaryDefault

        MealDetailActionKind.Disabled -> colors.bgActionSecondaryDisabled
        MealDetailActionKind.Danger -> colors.bgActionDangerDefault
    }
    val content = when (kind) {
        MealDetailActionKind.Secondary,
        MealDetailActionKind.SecondaryBorderless,
        -> colors.contentActionSecondary

        MealDetailActionKind.Disabled -> colors.contentActionSecondaryDisabled
        MealDetailActionKind.Danger -> colors.contentActionDanger
    }
    Surface(
        onClick = onClick,
        modifier = modifier.size(MealDetailActionWidth, MealDetailActionHeight),
        enabled = enabled,
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
        color = container,
        contentColor = content,
        border = if (kind == MealDetailActionKind.Secondary) {
            BorderStroke(MealDetailBorderWidth, colors.borderActionSecondary)
        } else {
            null
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            DandiText(
                text = label,
                color = content,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
                textAlign = TextAlign.Center,
            )
        }
    }
}

enum class NyummyMealNutritionState {
    Start,
    DailyGoal,
    Final,
}

@Immutable
data class NyummyNutrientProgress(
    val label: String,
    val dailyValueLabel: String,
    val goalValueLabel: String,
    val mealValueLabel: String,
    val dailyProgress: Float,
    val mealProgress: Float,
    val unit: String = "g",
)

@Immutable
data class NyummyMealNutritionData(
    val carbohydrate: NyummyNutrientProgress,
    val protein: NyummyNutrientProgress,
    val fat: NyummyNutrientProgress,
)

@Composable
fun NyummyMealNutritionIndicator(
    data: NyummyMealNutritionData,
    modifier: Modifier = Modifier,
    state: NyummyMealNutritionState = NyummyMealNutritionState.Final,
    coachCharacter: @Composable () -> Unit = {},
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier.size(MealNutritionWidth, MealNutritionHeight)) {
        DandiText(
            text = "영양 섭취 현황",
            modifier = Modifier.size(MealNutritionTitleWidth, MealNutritionTitleHeight),
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            overflow = TextOverflow.Clip,
        )
        NutritionLegendDot(
            color = colors.dataProgressDailyTotal,
            modifier = Modifier.offset(x = DailyLegendDotOffsetX, y = NutritionLegendDotOffsetY),
        )
        DandiText(
            text = "하루 누적",
            modifier = Modifier.offset(x = DailyLegendTextOffsetX, y = NutritionLegendTextOffsetY),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        NutritionLegendDot(
            color = colors.dataProgressMealContribution,
            modifier = Modifier.offset(x = MealLegendDotOffsetX, y = NutritionLegendDotOffsetY),
        )
        DandiText(
            text = "이 식사",
            modifier = Modifier.offset(x = MealLegendTextOffsetX, y = NutritionLegendTextOffsetY),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        NutrientProgressRow(
            nutrient = data.carbohydrate,
            state = state,
            modifier = Modifier.offset(y = FirstNutrientOffsetY),
        )
        NutrientProgressRow(
            nutrient = data.protein,
            state = state,
            modifier = Modifier.offset(y = SecondNutrientOffsetY),
        )
        NutrientProgressRow(
            nutrient = data.fat,
            state = state,
            modifier = Modifier.offset(y = ThirdNutrientOffsetY),
        )
        Box(
            modifier = Modifier
                .offset(x = CoachTailOffsetX, y = CoachTailOffsetY)
                .size(CoachTailSize)
                .rotate(CoachTailRotation)
                .background(colors.bgCoachBubble)
                .border(CoachBorderWidth, colors.borderCoachBubble),
        )
        Surface(
            modifier = Modifier
                .offset(x = CoachBubbleOffsetX, y = CoachBubbleOffsetY)
                .size(CoachBubbleWidth, CoachBubbleHeight),
            shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
            color = colors.bgCoachBubble,
            contentColor = colors.contentDefaultLevel0,
            border = BorderStroke(CoachBorderWidth, colors.borderCoachBubble),
        ) {
            Box(Modifier.fillMaxSize()) {
                DandiText(
                    text = "냐미가 알려줄게!",
                    modifier = Modifier.offset(x = CoachTextInsetX, y = CoachTitleOffsetY),
                    color = colors.contentNutritionLabel,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
                DandiText(
                    text = "연한 바는 하루 누적,\n진한 바는 이 식사 양이야!",
                    modifier = Modifier
                        .offset(x = CoachTextInsetX, y = CoachCopyOffsetY)
                        .width(CoachCopyWidth),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.voiceRegularM,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = CoachCharacterOffsetX, y = CoachCharacterOffsetY)
                .size(CoachCharacterWidth, CoachCharacterHeight),
            contentAlignment = Alignment.Center,
        ) {
            coachCharacter()
        }
    }
}

@Composable
private fun NutritionLegendDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(NutritionLegendDotSize)
            .background(color, DesignSystemThemeImpl.designSystemShape.pill),
    )
}

@Composable
private fun NutrientProgressRow(
    nutrient: NyummyNutrientProgress,
    state: NyummyMealNutritionState,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier.size(MealNutritionWidth, NutrientRowHeight)) {
        DandiText(
            text = nutrient.label,
            modifier = Modifier.size(MealNutritionWidth, NutrientLabelHeight),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        DandiText(
            text = "하루 ${nutrient.dailyValueLabel} / ${nutrient.goalValueLabel}${nutrient.unit}",
            modifier = Modifier
                .offset(y = NutrientDailyLabelOffsetY)
                .size(MealNutritionWidth, NutrientLabelHeight),
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Box(
            modifier = Modifier
                .offset(y = NutrientTrackOffsetY)
                .size(MealNutritionWidth, NutrientTrackHeight)
                .background(colors.bgProgressNutritionTrack, DesignSystemThemeImpl.designSystemShape.pill),
        ) {
            if (state != NyummyMealNutritionState.Start) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(nutrient.dailyProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(colors.dataProgressDailyTotal, DesignSystemThemeImpl.designSystemShape.pill),
                )
            }
            if (state == NyummyMealNutritionState.Final) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(nutrient.mealProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(colors.dataProgressMealContribution, DesignSystemThemeImpl.designSystemShape.pill),
                )
            }
        }
    }
}

enum class NyummyPhotoPickerState {
    Selected,
    Empty,
    Uploading,
    Error,
    Disabled,
}

@Composable
fun NyummyPhotoPicker(
    state: NyummyPhotoPickerState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    photo: (@Composable () -> Unit)? = null,
    contentDescription: String = "식사 사진",
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val status = when (state) {
        NyummyPhotoPickerState.Selected -> "첨부 완료"
        NyummyPhotoPickerState.Empty -> "사진을 추가해요"
        NyummyPhotoPickerState.Uploading -> "사진을 올리고 있어요"
        NyummyPhotoPickerState.Error -> "사진을 올리지 못했어요"
        NyummyPhotoPickerState.Disabled -> "처리 중에는 바꿀 수 없어요"
    }
    val action = when (state) {
        NyummyPhotoPickerState.Selected -> "사진 바꾸기"
        NyummyPhotoPickerState.Empty -> "사진 선택"
        NyummyPhotoPickerState.Uploading -> "업로드 중…"
        NyummyPhotoPickerState.Error -> "다시 선택"
        NyummyPhotoPickerState.Disabled -> "사진 바꾸기"
    }
    val statusColor = when (state) {
        NyummyPhotoPickerState.Selected -> colors.contentNutritionLabel

        NyummyPhotoPickerState.Empty -> colors.contentDefaultLevel1
        NyummyPhotoPickerState.Uploading -> colors.contentStatusProcessing
        NyummyPhotoPickerState.Error -> colors.contentError
        NyummyPhotoPickerState.Disabled -> colors.contentInputDisabled
    }
    val actionContainer = when (state) {
        NyummyPhotoPickerState.Selected,
        NyummyPhotoPickerState.Empty,
        -> colors.bgActionSecondaryDefault

        NyummyPhotoPickerState.Uploading,
        NyummyPhotoPickerState.Disabled,
        -> colors.bgActionSecondaryDisabled

        NyummyPhotoPickerState.Error -> colors.bgDangerSoft
    }
    val actionContent = when (state) {
        NyummyPhotoPickerState.Selected,
        NyummyPhotoPickerState.Empty,
        -> colors.contentActionSecondary

        NyummyPhotoPickerState.Uploading,
        NyummyPhotoPickerState.Disabled,
        -> colors.contentActionSecondaryDisabled

        NyummyPhotoPickerState.Error -> colors.contentError
    }
    val actionBorder = if (state == NyummyPhotoPickerState.Error) {
        colors.borderInputError
    } else {
        colors.borderActionSecondary
    }
    val actionEnabled = state != NyummyPhotoPickerState.Uploading && state != NyummyPhotoPickerState.Disabled
    val rootContainer = if (state == NyummyPhotoPickerState.Disabled) {
        colors.bgInputDisabled
    } else {
        colors.bgInputDefault
    }
    val rootBorder = if (state == NyummyPhotoPickerState.Error) {
        colors.borderInputError
    } else {
        colors.borderMealPhoto
    }
    val rootContent = if (state == NyummyPhotoPickerState.Disabled) {
        colors.contentInputDisabled
    } else {
        colors.contentDefaultLevel0
    }
    val rootShape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20)
    val photoShape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16)

    Surface(
        modifier = modifier
            .size(PhotoPickerWidth, PhotoPickerHeight)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = status
            },
        shape = rootShape,
        color = rootContainer,
        contentColor = rootContent,
        border = BorderStroke(PhotoPickerBorderWidth, rootBorder),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = PhotoPickerPhotoOffsetX, y = PhotoPickerPhotoOffsetY)
                    .size(PhotoPickerPhotoSize)
                    .clip(photoShape)
                    .background(
                        if (state == NyummyPhotoPickerState.Empty) {
                            colors.bgSurfaceCardSubtle
                        } else {
                            colors.bgMealPhoto
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null && state != NyummyPhotoPickerState.Empty) {
                    photo()
                } else if (state == NyummyPhotoPickerState.Empty) {
                    DandiText(
                        text = "사진 없음",
                        color = colors.contentNutritionLabel,
                        style = DesignSystemThemeImpl.typeScale.labelRegularXS,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            DandiText(
                text = "사진 · 필수",
                modifier = Modifier
                    .offset(x = PhotoPickerContentOffsetX, y = PhotoPickerLabelOffsetY)
                    .size(PhotoPickerLabelWidth, PhotoPickerLabelHeight),
                color = if (state == NyummyPhotoPickerState.Disabled) {
                    colors.contentInputDisabled
                } else {
                    colors.contentDefaultLevel1
                },
                style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            )
            DandiText(
                text = status,
                modifier = Modifier
                    .offset(x = PhotoPickerContentOffsetX, y = PhotoPickerStatusOffsetY)
                    .size(PhotoPickerLabelWidth, PhotoPickerStatusHeight),
                color = statusColor,
                style = DesignSystemThemeImpl.typeScale.textStrongXL,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                onClick = onClick,
                modifier = Modifier
                    .offset(x = PhotoPickerContentOffsetX, y = PhotoPickerActionOffsetY)
                    .size(PhotoPickerActionWidth, DesignSystemThemeImpl.designSystemSize.minimumTouchTarget),
                enabled = actionEnabled,
                shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius22),
                color = actionContainer,
                contentColor = actionContent,
                border = BorderStroke(PhotoPickerBorderWidth, actionBorder),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DandiText(
                        text = action,
                        color = actionContent,
                        style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private val CalendarDayWidth = 50.dp
private val CalendarDayHeight = 56.dp
private val CalendarDateOffsetY = 5.dp
private val CalendarDateHeight = 18.dp
private val CalendarFoodIconOffsetY = 21.dp
private val CalendarSingleIconOffsetX = 13.dp
private val CalendarFirstDoubleIconOffsetX = 4.dp
private val CalendarSecondDoubleIconOffsetX = 22.dp
private val CalendarMarkerOffsetX = 22.dp
private val CalendarMarkerOffsetY = 47.dp
private val CalendarBorderWidth = 1.dp
private val CalendarMarkerBorderWidth = 1.dp
private val PositiveCheckStroke = 0.8.dp
private val CalendarHeaderActionSize = 48.dp
private val CalendarHeaderActionRadius = 14.dp
private val CalendarChevronSize = 22.dp
private val CalendarChevronStroke = 2.dp

private val MealRowWidth = 350.dp
private val MealRowInnerHeight = 44.dp
private val MealRowCompletedContentWidth = 168.dp
private val MealRowStateContentWidth = 164.dp
private val MealRowCompletedTrailingWidth = 98.dp
private val MealRowStateTrailingWidth = 102.dp
private val MealRowBorderWidth = 1.dp
private val MealRowStateBorderWidth = 1.25.dp

private val AnalysisBannerWidth = 350.dp
private val AnalysisBannerHeight = 80.dp
private val AnalysisBannerHorizontalPadding = 10.dp
private val AnalysisGuideWidth = 38.dp
private val AnalysisGuideHeight = 40.dp
private val AnalysisBannerMessageHeight = 64.dp
private val AnalysisFailedMessageWidth = 192.dp
private val AnalysisRetryingMessageWidth = 284.dp
private val AnalysisRetryWidth = 84.dp

private val DailySummaryWidth = 350.dp
private val DailySummaryHeight = 112.dp
private val DailySummaryBorderWidth = 1.dp
private val DailySummaryHorizontalInset = 16.dp
private val DailySummaryTitleOffsetY = 10.dp
private val DailySummaryTitleWidth = 150.dp
private val DailySummaryTitleHeight = 24.dp
private val DailySummaryNoteOffsetX = 184.dp
private val DailySummaryNoteWidth = 150.dp
private val DailySummaryCalorieOffsetY = 36.dp
private val DailySummaryCalorieWidth = 230.dp
private val DailySummaryRatioOffsetX = 272.dp
private val DailySummaryRatioOffsetY = 35.dp
private val DailySummaryRatioWidth = 62.dp
private val DailySummaryRatioHeight = 26.dp
private val DailyMacroOffsetY = 67.dp
private val DailyMacroFirstOffsetX = 16.dp
private val DailyMacroSecondOffsetX = 122.dp
private val DailyMacroThirdOffsetX = 228.dp
private val DailyMacroWidth = 96.dp
private val DailyMacroHeight = 33.dp
private val DailyMacroLabelHeight = 20.dp
private val DailyMacroTrackOffsetY = 27.dp
private val DailyMacroTrackHeight = 6.dp

private val MealDetailWidth = 656.dp
private val MealDetailHeight = 306.dp
private val MealDetailInset = 16.dp
private val MealDetailBorderWidth = 1.dp
private val MealDetailPhotoSize = 176.dp
private val MealDetailRequiredPillOffsetY = 48.dp
private val MealDetailRequiredPillWidth = 92.dp
private val MealDetailPhotoLabelOffsetY = 60.dp
private val MealDetailPhotoLabelWidth = 152.dp
private val MealDetailContentOffsetX = 214.dp
private val MealDetailFoodIconOffsetY = 20.dp
private val MealDetailOrderOffsetX = 270.dp
private val MealDetailOrderOffsetY = 20.dp
private val MealDetailOrderWidth = 190.dp
private val MealDetailStatusTextHeight = 18.dp
private val MealDetailNameOffsetY = 62.dp
private val MealDetailNameWidth = 300.dp
private val MealDetailNameHeight = 26.dp
private val MealDetailCaloriesOffsetY = 98.dp
private val MealDetailCaloriesWidth = 72.dp
private val MealDetailCaloriesHeight = 30.dp
private val MealDetailCalorieUnitOffsetX = 292.dp
private val MealDetailCalorieUnitOffsetY = 108.dp
private val MealDetailCalorieUnitWidth = 60.dp
private val MealDetailCapturePillOffsetY = 148.dp
private val MealDetailCapturePillWidth = 188.dp
private val MealDetailFailedCapturePillWidth = 240.dp
private val MealDetailTimestampOffsetY = 184.dp
private val MealDetailTimestampWidth = 390.dp
private val MealDetailStatusOffsetX = 500.dp
private val MealDetailStatusWidth = 138.dp
private val MealDetailStatusHeight = 30.dp
private val MealDetailActionsOffsetY = 226.dp
private val MealDetailFirstActionOffsetX = 16.dp
private val MealDetailSecondActionOffsetX = 224.dp
private val MealDetailThirdActionOffsetX = 432.dp
private val MealDetailActionWidth = 192.dp
private val MealDetailActionHeight = 52.dp

private val MealNutritionWidth = 302.dp
private val MealNutritionHeight = 326.dp
private val MealNutritionTitleWidth = 158.dp
private val MealNutritionTitleHeight = 22.dp
private val DailyLegendDotOffsetX = 168.dp
private val MealLegendDotOffsetX = 240.dp
private val NutritionLegendDotOffsetY = 9.dp
private val NutritionLegendDotSize = 6.dp
private val DailyLegendTextOffsetX = 178.dp
private val MealLegendTextOffsetX = 250.dp
private val NutritionLegendTextOffsetY = 3.dp
private val FirstNutrientOffsetY = 36.dp
private val SecondNutrientOffsetY = 104.dp
private val ThirdNutrientOffsetY = 172.dp
private val NutrientRowHeight = 50.dp
private val NutrientLabelHeight = 18.dp
private val NutrientDailyLabelOffsetY = 19.dp
private val NutrientTrackOffsetY = 42.dp
private val NutrientTrackHeight = 8.dp
private val CoachTailOffsetX = 55.dp
private val CoachTailOffsetY = 271.dp
private val CoachTailSize = 14.dp
private const val CoachTailRotation = 45f
private val CoachBubbleOffsetX = 58.dp
private val CoachBubbleOffsetY = 244.dp
private val CoachBubbleWidth = 244.dp
private val CoachBubbleHeight = 78.dp
private val CoachBorderWidth = 1.dp
private val CoachTextInsetX = 18.dp
private val CoachTitleOffsetY = 11.dp
private val CoachCopyOffsetY = 31.dp
private val CoachCopyWidth = 208.dp
private val CoachCharacterOffsetX = 0.dp
private val CoachCharacterOffsetY = 258.dp
private val CoachCharacterWidth = 62.dp
private val CoachCharacterHeight = 66.dp

private val PhotoPickerWidth = 350.dp
private val PhotoPickerHeight = 166.dp
private val PhotoPickerBorderWidth = 1.dp
private val PhotoPickerPhotoOffsetX = 12.dp
private val PhotoPickerPhotoOffsetY = 15.dp
private val PhotoPickerPhotoSize = 136.dp
private val PhotoPickerContentOffsetX = 166.dp
private val PhotoPickerLabelOffsetY = 20.dp
private val PhotoPickerLabelWidth = 150.dp
private val PhotoPickerLabelHeight = 20.dp
private val PhotoPickerStatusOffsetY = 42.dp
private val PhotoPickerStatusHeight = 34.dp
private val PhotoPickerActionOffsetY = 86.dp
private val PhotoPickerActionWidth = 166.dp

@Preview(showBackground = true)
@Composable
private fun NyummyHistoryComponentsPreview() {
    DesignSystemTheme {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16),
        ) {
            NyummyMealRow(
                data = NyummyMealRowData(
                    orderLabel = "첫 끼",
                    name = "치킨 샐러드",
                    recordedMeta = "08:10 · 사진 기록",
                    calories = "412 kcal",
                ),
                state = NyummyAnalysisState.Completed,
                onClick = {},
            )
            NyummyAnalysisBanner(
                title = "세 번째 끼니 분석이 멈췄어",
                message = "기록은 그대로야. 다시 해볼까?",
                state = NyummyAnalysisBannerState.Failed,
            )
            NyummyPhotoPicker(state = NyummyPhotoPickerState.Empty, onClick = {})
        }
    }
}

@Preview(widthDp = 700, showBackground = true)
@Composable
private fun NyummyMealDetailPreview() {
    DesignSystemTheme {
        NyummyMealDetailCard(
            data = NyummyMealDetailData(
                orderAndTime = "첫 끼 · 12:36",
                name = "닭가슴살 포케",
                calories = "540",
                capturedAt = "2026.07.12 12:36",
            ),
        )
    }
}
