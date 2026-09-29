package com.dandi.nyummy.history.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyDestructiveDialog
import com.dandi.nyummy.common.presentation.component.NyummyEditDialog
import com.dandi.nyummy.common.presentation.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyMascot
import com.dandi.nyummy.common.presentation.component.NyummyMascotPose
import com.dandi.nyummy.common.presentation.component.NyummyModalScrim
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.history.entity.NutrientProgressVO
import com.dandi.nyummy.history.presentation.HistoryIntent
import com.dandi.nyummy.history.presentation.HistoryMealDetailMode
import com.dandi.nyummy.history.presentation.HistoryMealDetailUiState
import com.dandi.nyummy.history.presentation.R
import com.dandi.nyummy.history.presentation.model.dayLabelOf
import com.dandi.nyummy.history.presentation.model.mealOrderLabelOf
import com.dandi.nyummy.history.presentation.model.meridiemTimeOf
import com.dandi.nyummy.history.presentation.model.numberLabelOf
import com.dandi.nyummy.history.presentation.model.percentOf

/**
 * 히스토리 위에 뜨는 식사 상세 오버레이입니다.
 *
 * 보기 모드에서는 화면 높이를 채우는 상세 카드를, 이름 수정/삭제 확인 모드에서는 해당 다이얼로그를 보여줍니다.
 * 페이지 영역은 루트가 바텀 네비 위까지로 잘라 주므로, 여기서는 위아래 같은 숨 고르기 여백만 둡니다.
 */
@Composable
internal fun HistoryMealDetailOverlay(
    detail: HistoryMealDetailUiState,
    selectedDate: HistoryDateVO,
    mealCount: Int,
    dailyNutrition: DailyNutritionVO,
    onIntent: (HistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        NyummyModalScrim(
            onDismissRequest = when (detail.mode) {
                HistoryMealDetailMode.Viewing -> {
                    { onIntent(HistoryIntent.DismissMealDetail) }
                }

                HistoryMealDetailMode.EditingName,
                HistoryMealDetailMode.ConfirmingDelete,
                -> null
            },
        )
        when (detail.mode) {
            HistoryMealDetailMode.Viewing -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = OverlayHorizontalInset, vertical = OverlayVerticalInset),
                contentAlignment = Alignment.Center,
            ) {
                HistoryMealDetailCard(
                    meal = detail.meal,
                    selectedDate = selectedDate,
                    mealCount = mealCount,
                    dailyNutrition = dailyNutrition,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxHeight(),
                )
            }

            HistoryMealDetailMode.EditingName -> CenteredDialogHost {
                NyummyEditDialog(
                    title = stringResource(R.string.history_edit_dialog_title),
                    fieldLabel = stringResource(R.string.history_edit_dialog_field_label),
                    fieldValue = detail.nameDraft,
                    onFieldValueChange = { onIntent(HistoryIntent.ChangeMealNameDraft(it)) },
                    timeLabel = stringResource(
                        R.string.history_edit_dialog_time_label,
                        detail.meal.recordedAt,
                    ),
                    cancelLabel = stringResource(R.string.history_edit_dialog_cancel),
                    confirmLabel = stringResource(R.string.history_edit_dialog_confirm),
                    onCancel = { onIntent(HistoryIntent.CancelEditMealName) },
                    onConfirm = { onIntent(HistoryIntent.ConfirmEditMealName) },
                )
            }

            HistoryMealDetailMode.ConfirmingDelete -> CenteredDialogHost {
                NyummyDestructiveDialog(
                    title = stringResource(R.string.history_delete_dialog_title),
                    body = stringResource(R.string.history_delete_dialog_body),
                    // 분석 실패 기록은 이름이 비어 있어 대체 문구로 무엇을 지우는지 알려준다.
                    targetLabel = "${mealTitleOf(detail.meal)} · ${detail.meal.recordedAt}",
                    helper = stringResource(R.string.history_delete_dialog_helper),
                    cancelLabel = stringResource(R.string.history_delete_dialog_cancel),
                    confirmLabel = stringResource(R.string.history_delete_dialog_confirm),
                    onCancel = { onIntent(HistoryIntent.CancelDeleteMeal) },
                    onConfirm = { onIntent(HistoryIntent.ConfirmDeleteMeal) },
                )
            }
        }
    }
}

/**
 * 이름 수정/삭제 확인처럼 작은 다이얼로그를 화면 가운데에 놓는 껍데기입니다.
 * 키보드가 올라오거나 화면이 작아 다이얼로그가 다 안 들어가면 스크롤로 전부 볼 수 있습니다.
 */
@Composable
private fun CenteredDialogHost(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = OverlayVerticalInset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = { content() },
    )
}

/** 카드·다이얼로그에 보여줄 식사 이름입니다. 분석 전/실패라 이름이 없으면 대체 문구를 씁니다. */
@Composable
private fun mealTitleOf(meal: MealHistoryVO): String =
    meal.name.ifBlank { stringResource(R.string.history_analysis_unknown_meal) }

/**
 * 상세 카드. 머리(끼니 라벨·이름·닫기)와 바닥(수정/삭제 액션)은 고정하고, 그 사이 내용만 스크롤합니다.
 *
 * 호출부가 높이를 채워 주므로 카드 크기는 화면에 맞춰 고정되고, 내용이 길면 카드 안에서만 스크롤됩니다.
 * 내용 순서: 사진 배너 → 냐미 한마디 → 열량 요약 카드 → 영양 섭취 현황 카드.
 */
@Composable
private fun HistoryMealDetailCard(
    meal: MealHistoryVO,
    selectedDate: HistoryDateVO,
    mealCount: Int,
    dailyNutrition: DailyNutritionVO,
    onIntent: (HistoryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius32)
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier
            // 좁은 화면(320dp)에서는 가로 여백을 남기고 줄어들고, 넓은 화면에서는 기본 폭에서 멈춘다.
            .widthIn(max = DetailCardWidth)
            .fillMaxWidth()
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.dialogStandard,
            ),
        shape = shape,
        color = colors.bgSurfaceIvory,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Column {
            DetailHeader(
                meal = meal,
                selectedDate = selectedDate,
                mealCount = mealCount,
                onClose = { onIntent(HistoryIntent.DismissMealDetail) },
            )
            // 내용이 머리 아래로 스크롤되어 들어가기 시작하면 경계선을 드러내 겹침을 정리한다.
            SectionEdgeDivider(visible = { scrollState.canScrollBackward })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = DetailCardInset),
            ) {
                DetailPhotoBanner(meal = meal)
                Spacer(Modifier.height(DetailSectionGap))
                DetailCatComment(comment = meal.catComment)
                Spacer(Modifier.height(DetailSectionGap))
                DetailCalorieSummary(meal = meal, dailyNutrition = dailyNutrition)
                Spacer(Modifier.height(DetailSectionGap))
                DetailInnerCard {
                    HistoryMealNutritionSection(nutrition = dailyNutrition, meal = meal)
                }
                Spacer(Modifier.height(DetailSectionGap))
            }
            // 아래에 더 볼 내용이 남아 있을 때만 바닥 액션 위에 경계선을 그린다.
            SectionEdgeDivider(visible = { scrollState.canScrollForward })
            Row(
                modifier = Modifier.padding(
                    start = DetailCardInset,
                    top = DetailActionTopInset,
                    end = DetailCardInset,
                    bottom = DetailActionBottomInset,
                ),
                horizontalArrangement = Arrangement.spacedBy(DetailActionGap),
            ) {
                NyummyButton(
                    label = stringResource(R.string.history_detail_edit_name),
                    modifier = Modifier.weight(1f),
                    style = NyummyButtonStyle.Secondary,
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                    onClick = { onIntent(HistoryIntent.ClickEditMealName) },
                )
                NyummyButton(
                    label = stringResource(R.string.history_detail_delete),
                    modifier = Modifier.weight(1f),
                    style = NyummyButtonStyle.Danger,
                    leadingIcon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
                    onClick = { onIntent(HistoryIntent.ClickDeleteMeal) },
                )
            }
        }
    }
}

/**
 * 고정 영역(머리·바닥)과 스크롤 영역의 경계선입니다.
 * 자리는 항상 차지하고 [visible]일 때만 보여 레이아웃이 튀지 않습니다.
 * 스크롤 상태는 graphicsLayer 블록 안에서 읽어, 스크롤마다 리컴포지션 없이 보임/숨김만 바뀝니다.
 */
@Composable
private fun SectionEdgeDivider(visible: () -> Boolean) {
    HorizontalDivider(
        modifier = Modifier.graphicsLayer { alpha = if (visible()) 1f else 0f },
        thickness = DetailDividerWidth,
        color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
    )
}

/** 상세 카드 안에 들어가는 흰 배경 + 옅은 테두리 카드 껍데기(열량 요약·영양 섭취 현황이 공유). */
@Composable
private fun DetailInnerCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20),
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
        border = BorderStroke(DetailInnerCardBorderWidth, colors.borderCardSubtle),
    ) {
        Box(modifier = Modifier.padding(DetailInnerCardInset)) { content() }
    }
}

@Composable
private fun DetailHeader(
    meal: MealHistoryVO,
    selectedDate: HistoryDateVO,
    mealCount: Int,
    onClose: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(
        modifier = Modifier.padding(
            start = DetailCardInset,
            top = DetailHeaderTopInset,
            end = DetailHeaderEndInset,
            bottom = DetailHeaderBottomGap,
        ),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_icon_leaf),
                    contentDescription = null,
                    modifier = Modifier.size(DetailHeaderLeafSize),
                    tint = colors.contentAccentSage,
                )
                Spacer(Modifier.width(DetailHeaderLeafGap))
                DandiText(
                    text = "${dayLabelOf(selectedDate)} · ${mealOrderLabelOf(meal.orderIndex, mealCount)}",
                    color = colors.contentAccentSage,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
            }
            Spacer(Modifier.height(DetailTitleTopGap))
            DandiText(
                text = mealTitleOf(meal),
                color = colors.contentDefaultLevel0,
                maxLines = 2,
                style = DesignSystemThemeImpl.typeScale.displayRegularXL,
            )
        }
        Spacer(Modifier.width(DetailHeaderCloseGap))
        NyummyIconButton(
            contentDescription = stringResource(R.string.history_detail_close),
            style = NyummyIconButtonStyle.Filled,
            onClick = onClose,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
            )
        }
    }
}

/**
 * 촬영 사진을 가로로 넓게 보여주고, 왼쪽 아래에 시계 아이콘 + 촬영 시각 칩을 얹습니다.
 * 사진 URL 이 없거나 로드에 실패하면(만료된 presigned URL·삭제된 원본) 음식 아이콘으로 대체합니다.
 */
@Composable
private fun DetailPhotoBanner(
    meal: MealHistoryVO,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    var photoLoadFailed by remember(meal.photoUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DetailPhotoHeight)
            .clip(RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20))
            .background(colors.bgMealPhoto),
    ) {
        if (meal.photoUrl.isBlank() || photoLoadFailed) {
            Box(
                modifier = Modifier
                    .size(DetailPhotoFallbackSize)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                HistoryFoodIcon(meal.foodIconId, sizeFraction = DetailPhotoFallbackFraction)
            }
        } else {
            AsyncImage(
                model = meal.photoUrl,
                // 머리의 식사 이름 텍스트가 이미 안내하므로 장식 요소로 둔다(중복 낭독 방지).
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) photoLoadFailed = true
                },
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(DetailTimeChipInset)
                .designSystemDropShadow(
                    shape = DesignSystemThemeImpl.designSystemShape.pill,
                    shadow = DesignSystemThemeImpl.designSystemElevation.surfaceLow,
                ),
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            color = colors.bgSurfaceIvory,
            contentColor = colors.contentDefaultLevel0,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = DetailTimeChipHorizontalPadding,
                    vertical = DetailTimeChipVerticalPadding,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClockIcon(
                    color = colors.contentDefaultLevel0,
                    modifier = Modifier.size(DetailTimeChipIconSize),
                )
                Spacer(Modifier.width(DetailTimeChipIconGap))
                DandiText(
                    text = meridiemTimeOf(meal.recordedAt),
                    color = colors.contentDefaultLevel0,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
            }
        }
    }
}

/** 원 테두리 + 시침/분침으로 그린 시계 아이콘. */
@Composable
private fun ClockIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * ClockStrokeFraction
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = color, radius = (size.minDimension - stroke) / 2f, style = Stroke(width = stroke))
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - size.height * ClockHourHandFraction),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + size.width * ClockMinuteHandFraction, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * 냐미가 이 식사에 남긴 피드백 카드. 카드 왼쪽 윗변에 냐미가 앞발을 걸치고 매달린 모습으로 얹히고, 안에는 코멘트만 둡니다.
 * 코멘트는 줄 수 제한 없이 전부 보여주고(카드가 세로로 늘어남), 상세 응답이 아직 없거나 서버가 코멘트를
 * 주지 않으면 기본 인사말을 보여줘 레이아웃이 튀지 않게 합니다.
 */
@Composable
private fun DetailCatComment(
    comment: String,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // 카드 위로 튀어나온 냐미 머리만큼 카드를 아래로 내린다.
                .padding(top = DetailMascotOverhang),
            shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius24),
            color = colors.bgCoachBubble,
            contentColor = colors.contentDefaultLevel0,
            border = BorderStroke(DetailCommentCardBorderWidth, colors.borderCoachBubble),
        ) {
            Column(
                modifier = Modifier.padding(
                    start = DetailCommentCardHorizontalPadding,
                    top = DetailCommentCardTopPadding,
                    end = DetailCommentCardHorizontalPadding,
                    bottom = DetailCommentCardBottomPadding,
                ),
            ) {
                DandiText(
                    text = comment.ifBlank { stringResource(R.string.history_detail_cat_comment_empty) },
                    color = colors.contentDefaultLevel0,
                    // DandiText 기본값은 1줄 말줄임이라, 코멘트는 길이와 상관없이 전부 펼친다.
                    maxLines = Int.MAX_VALUE,
                    style = DesignSystemThemeImpl.typeScale.voiceRegularM,
                )
            }
        }
        // Surface 뒤에 선언해 카드 위에 그려진다. 앞발이 카드 윗변에 걸치도록 머리 부분만 카드 밖으로 내보낸다.
        NyummyMascot(
            pose = NyummyMascotPose.Hanging,
            contentDescription = stringResource(R.string.history_detail_mascot_description),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = DetailMascotStartInset)
                .size(width = DetailMascotWidth, height = DetailMascotHeight),
        )
    }
}

/** 이 식사 열량(왼쪽)과 하루 목표 대비 누적 열량 칩(오른쪽)을 한 카드에 둡니다. */
@Composable
private fun DetailCalorieSummary(
    meal: MealHistoryVO,
    dailyNutrition: DailyNutritionVO,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    DetailInnerCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DandiText(
                        text = CalorieEmoji,
                        style = DesignSystemThemeImpl.typeScale.textStrongL,
                    )
                    Spacer(Modifier.width(DetailCalorieEmojiGap))
                    DandiText(
                        text = stringResource(R.string.history_detail_calorie_label),
                        color = colors.contentNutritionLabel,
                        style = DesignSystemThemeImpl.typeScale.textRegularS,
                    )
                }
                Spacer(Modifier.height(DetailCalorieLabelGap))
                Row(verticalAlignment = Alignment.Bottom) {
                    DandiText(
                        text = numberLabelOf(meal.calorieKcal),
                        color = colors.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.numberStrongL,
                    )
                    Spacer(Modifier.width(DetailCalorieUnitGap))
                    DandiText(
                        text = "kcal",
                        modifier = Modifier.padding(bottom = DetailCalorieUnitLift),
                        color = colors.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.textStrongL,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
                color = colors.bgSuccessSoft,
                contentColor = colors.contentDefaultLevel0,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = DetailGoalChipInset,
                        vertical = DetailGoalChipVerticalInset,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HistoryBarChartIcon(modifier = Modifier.size(DetailGoalChipIconSize))
                    Spacer(Modifier.width(DetailGoalChipIconGap))
                    Column {
                        DandiText(
                            text = "하루 ${numberLabelOf(dailyNutrition.currentCalorieKcal)} / " +
                                numberLabelOf(dailyNutrition.targetCalorieKcal),
                            color = colors.contentNutritionLabel,
                            style = DesignSystemThemeImpl.typeScale.labelRegularXS,
                        )
                        Spacer(Modifier.height(DetailGoalChipGap))
                        DandiText(
                            text = "목표의 ${
                                percentOf(dailyNutrition.currentCalorieKcal, dailyNutrition.targetCalorieKcal)
                            }%",
                            color = colors.contentSuccess,
                            style = DesignSystemThemeImpl.typeScale.textStrongL,
                        )
                    }
                }
            }
        }
    }
}

private const val CalorieEmoji = "🔥"
private const val ClockStrokeFraction = 0.12f
private const val ClockHourHandFraction = 0.27f
private const val ClockMinuteHandFraction = 0.22f

private val OverlayVerticalInset = 24.dp
private val OverlayHorizontalInset = 16.dp
private val DetailCardWidth = 342.dp
private val DetailCardInset = 16.dp
private val DetailSectionGap = 14.dp
private val DetailDividerWidth = 1.dp
private val DetailInnerCardBorderWidth = 1.dp
private val DetailInnerCardInset = 16.dp
private val DetailHeaderTopInset = 16.dp
private val DetailHeaderEndInset = 12.dp
private val DetailHeaderBottomGap = 12.dp
private val DetailHeaderCloseGap = 8.dp
private val DetailHeaderLeafSize = 14.dp
private val DetailHeaderLeafGap = 4.dp
private val DetailTitleTopGap = 4.dp
private val DetailPhotoHeight = 200.dp
private val DetailPhotoFallbackSize = 96.dp
private const val DetailPhotoFallbackFraction = 0.75f
private val DetailTimeChipInset = 12.dp
private val DetailTimeChipHorizontalPadding = 14.dp
private val DetailTimeChipVerticalPadding = 8.dp
private val DetailTimeChipIconSize = 14.dp
private val DetailTimeChipIconGap = 6.dp
// 매달린 냐미 일러스트는 3:2 비율. 앞발이 걸친 턱선이 이미지 높이의 약 89% 지점에 있다.
private val DetailMascotWidth = 96.dp
private val DetailMascotHeight = 64.dp
// 이미지 속 턱선(높이의 약 89%)이 카드 윗변에 오도록 그 위쪽만 카드 밖으로 내보낸다. 앞발 끝 6dp 정도가 카드에 걸친다.
private val DetailMascotOverhang = 58.dp
private val DetailMascotStartInset = 20.dp
private val DetailCommentCardBorderWidth = 1.dp
private val DetailCommentCardHorizontalPadding = 20.dp
private val DetailCommentCardTopPadding = 20.dp
private val DetailCommentCardBottomPadding = 20.dp
private val DetailCalorieEmojiGap = 6.dp
private val DetailCalorieLabelGap = 4.dp
private val DetailCalorieUnitGap = 4.dp
private val DetailCalorieUnitLift = 3.dp
private val DetailGoalChipInset = 12.dp
private val DetailGoalChipVerticalInset = 10.dp
private val DetailGoalChipIconSize = 18.dp
private val DetailGoalChipIconGap = 8.dp
private val DetailGoalChipGap = 2.dp
private val DetailActionTopInset = 12.dp
private val DetailActionBottomInset = 16.dp
private val DetailActionGap = 10.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 860)
@Composable
private fun HistoryMealDetailOverlayPreview() {
    DesignSystemTheme {
        HistoryMealDetailOverlay(
            detail = HistoryMealDetailUiState(
                meal = MealHistoryVO(
                    id = "preview-1",
                    name = "치킨 샐러드",
                    foodIconId = "salad",
                    recordedAt = "08:10",
                    calorieKcal = 412,
                    carbohydrateGram = 18,
                    proteinGram = 42,
                    fatGram = 21,
                    orderIndex = 1,
                    catComment = "채소랑 단백질이 골고루 들어 있어서 아침으로 딱 좋아!",
                ),
            ),
            selectedDate = HistoryDateVO(2026, 7, 18),
            mealCount = 5,
            dailyNutrition = DailyNutritionVO(
                currentCalorieKcal = 2_129,
                targetCalorieKcal = 2_000,
                carbohydrate = NutrientProgressVO(dailyGram = 265, goalGram = 300),
                protein = NutrientProgressVO(dailyGram = 124, goalGram = 120),
                fat = NutrientProgressVO(dailyGram = 68, goalGram = 70),
            ),
            onIntent = {},
        )
    }
}
