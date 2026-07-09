package com.swm.dandi.meal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swm.dandi.common.presentation.R
import com.swm.dandi.common.presentation.component.ArchiText
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl
import com.swm.dandi.meal.entity.MealSessionStatusTypeVO
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class MealStatusSheetUiState(
    val step: MealStatusSheetStep = MealStatusSheetStep.Status,
    val mealSessions: ImmutableList<MealSessionCardUiState> = DefaultMealSessions,
    val nutrients: ImmutableList<NutrientProgressUiState> = DefaultNutrients,
)

enum class MealStatusSheetStep {
    Status,
    RecordType,
}

data class MealSessionCardUiState(
    val mealRecordId: String = "",
    val mealType: String,
    val statusType: MealSessionStatusTypeVO = MealSessionStatusTypeVO.UNKNOWN,
    val title: String,
    val status: String,
    val displayName: String = "",
    val description: String,
    val isError: Boolean = false,
    val canRetryAnalysis: Boolean = false,
)

data class NutrientProgressUiState(
    val nutrientType: String,
    val label: String,
    val percent: String,
    val progress: Float,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealStatusSheet(
    uiState: MealStatusSheetUiState,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onRecordClick: () -> Unit = {},
    onRetryMealAnalysis: (String) -> Unit = {},
    onPreviousMealClick: () -> Unit = {},
    onNewFoodClick: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0,
        contentColor = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
        scrimColor = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel0.copy(alpha = 0.32f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { MealStatusDragHandle() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
        ) {
            when (uiState.step) {
                MealStatusSheetStep.Status -> MealStatusSheetContent(
                    uiState = uiState,
                    onRecordClick = onRecordClick,
                    onRetryMealAnalysis = onRetryMealAnalysis,
                )

                MealStatusSheetStep.RecordType -> MealRecordTypeSelectionContent(
                    onPreviousMealClick = onPreviousMealClick,
                    onNewFoodClick = onNewFoodClick,
                )
            }
        }
    }
}

@Composable
private fun MealStatusSheetContent(
    uiState: MealStatusSheetUiState,
    modifier: Modifier = Modifier,
    onRecordClick: () -> Unit,
    onRetryMealAnalysis: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 28.dp),
    ) {
        Spacer(modifier = Modifier.height(22.dp))
        ArchiText(
            text = stringResource(R.string.meal_status_title),
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ArchiText(
            text = stringResource(R.string.meal_status_subtitle),
            style = DesignSystemThemeImpl.typeScale.textRegularL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(34.dp))
        MealSessionGrid(
            mealSessions = uiState.mealSessions,
            onRetryMealAnalysis = onRetryMealAnalysis,
        )
        Spacer(modifier = Modifier.height(28.dp))
        MealStatusPrimaryButton(
            text = stringResource(R.string.meal_status_new_record),
            onClick = onRecordClick,
        )
        Spacer(modifier = Modifier.height(24.dp))
        NutritionSummary(uiState = uiState)
    }
}

@Composable
private fun MealSessionGrid(
    mealSessions: ImmutableList<MealSessionCardUiState>,
    modifier: Modifier = Modifier,
    onRetryMealAnalysis: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        mealSessions.chunked(MEAL_SESSION_GRID_COLUMN_COUNT).forEach { rowSessions ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowSessions.forEach { session ->
                    MealSessionCard(
                        title = session.title,
                        status = session.status,
                        desc = session.description,
                        isError = session.isError,
                        canRetryAnalysis = session.canRetryAnalysis,
                        onRetryAnalysis = { onRetryMealAnalysis(session.mealRecordId) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowSessions.size < MEAL_SESSION_GRID_COLUMN_COUNT) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MealStatusDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1),
        )
    }
}

@Composable
private fun MealSessionCard(
    title: String,
    status: String,
    desc: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    canRetryAnalysis: Boolean = false,
    onRetryAnalysis: () -> Unit = {},
) {
    val borderColor = if (isError) {
        DesignSystemThemeImpl.designSystemColor.contentFavorite.copy(alpha = 0.35f)
    } else {
        DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
    }
    Surface(
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isError) {
            DesignSystemThemeImpl.designSystemColor.borderDefaultLevel2.copy(alpha = 0.65f)
        } else {
            DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
        },
        border = BorderStroke(width = 1.dp, color = borderColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArchiText(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = DesignSystemThemeImpl.typeScale.textStrongL,
                    color = if (isError) {
                        DesignSystemThemeImpl.designSystemColor.contentFavorite
                    } else {
                        DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1
                    },
                    maxLines = 1,
                )
                if (canRetryAnalysis) {
                    RetryAnalysisIconButton(onClick = onRetryAnalysis)
                }
            }
            ArchiText(
                text = status,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                color = if (isError) {
                    DesignSystemThemeImpl.designSystemColor.contentFavorite
                } else {
                    DesignSystemThemeImpl.designSystemColor.contentAccent
                },
                maxLines = 1,
            )
            ArchiText(
                text = desc,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
                color = if (isError) {
                    DesignSystemThemeImpl.designSystemColor.contentFavorite
                } else {
                    DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RetryAnalysisIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(DesignSystemThemeImpl.designSystemColor.contentFavorite.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "분석 다시 시도",
            tint = DesignSystemThemeImpl.designSystemColor.contentFavorite,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MealRecordTypeSelectionContent(
    onPreviousMealClick: () -> Unit,
    onNewFoodClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(22.dp))
        ArchiText(
            text = stringResource(R.string.meal_status_record_type_title),
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ArchiText(
            text = stringResource(R.string.meal_status_record_type_subtitle),
            style = DesignSystemThemeImpl.typeScale.textRegularL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(36.dp))
        MealRecordTypeOptionCard(
            title = stringResource(R.string.meal_entry_previous_title),
            desc = stringResource(R.string.meal_entry_previous_desc),
            icon = Icons.Filled.History,
            onClick = onPreviousMealClick,
        )
        Spacer(modifier = Modifier.height(18.dp))
        MealRecordTypeOptionCard(
            title = stringResource(R.string.meal_entry_new_title),
            desc = stringResource(R.string.meal_entry_new_desc),
            icon = Icons.Filled.AddAPhoto,
            accent = true,
            onClick = onNewFoodClick,
        )
    }
}

@Composable
private fun MealRecordTypeOptionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        border = BorderStroke(
            width = 1.dp,
            color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MealRecordTypeIconTile(icon = icon, accent = accent)
            Spacer(modifier = Modifier.width(18.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArchiText(
                    text = title,
                    style = DesignSystemThemeImpl.typeScale.titleStrongL,
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ArchiText(
                    text = desc,
                    style = DesignSystemThemeImpl.typeScale.textRegularM,
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun MealRecordTypeIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(66.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (accent) {
                    DesignSystemThemeImpl.designSystemColor.contentAccent.copy(alpha = 0.72f)
                } else {
                    DesignSystemThemeImpl.designSystemColor.borderDefaultLevel2
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) {
                DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
            } else {
                DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
            },
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun MealStatusPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(DesignSystemThemeImpl.designSystemColor.contentAccent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            ArchiText(
                text = text,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
                color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NutritionSummary(
    uiState: MealStatusSheetUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        border = BorderStroke(
            width = 1.dp,
            color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2.copy(alpha = 0.65f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArchiText(
                text = stringResource(R.string.meal_status_nutrition_title),
                style = DesignSystemThemeImpl.typeScale.textStrongL,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                maxLines = 1,
            )
            uiState.nutrients.forEach { nutrient ->
                NutritionProgress(
                    label = nutrient.label,
                    percent = nutrient.percent,
                    progress = nutrient.progress,
                )
            }
        }
    }
}

@Composable
private fun NutritionProgress(
    label: String,
    percent: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchiText(
                text = label,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                maxLines = 1,
            )
            ArchiText(
                text = percent,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                maxLines = 1,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            trackColor = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel2,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealStatusSheetContentPreview() {
    DesignSystemTheme {
        MealStatusSheetContent(
            uiState = MealStatusSheetUiState(),
            onRecordClick = {},
            onRetryMealAnalysis = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealRecordTypeSelectionContentPreview() {
    DesignSystemTheme {
        MealRecordTypeSelectionContent(
            onPreviousMealClick = {},
            onNewFoodClick = {},
        )
    }
}

private const val MEAL_SESSION_GRID_COLUMN_COUNT = 2

private val DefaultMealSessions = persistentListOf(
    MealSessionCardUiState(
        mealRecordId = "meal_breakfast_001",
        mealType = "BREAKFAST",
        statusType = MealSessionStatusTypeVO.RECORDED,
        title = "아침",
        status = "기록됨",
        displayName = "흰 쌀밥",
        description = "흰 쌀밥",
    ),
    MealSessionCardUiState(
        mealType = "LUNCH",
        statusType = MealSessionStatusTypeVO.NOT_RECORDED,
        title = "점심",
        status = "기록 전",
        description = "식사를 기록해주세요",
    ),
    MealSessionCardUiState(
        mealRecordId = "meal_skeleton_001",
        mealType = "DINNER",
        statusType = MealSessionStatusTypeVO.PENDING,
        title = "저녁",
        status = "분석 대기",
        displayName = "닭가슴살 샐러드",
        description = "닭가슴살 샐러드 분석 대기 중...",
    ),
    MealSessionCardUiState(
        mealRecordId = "meal_snack_failed_001",
        mealType = "SNACK",
        statusType = MealSessionStatusTypeVO.FAILED,
        title = "간식",
        status = "분석 실패",
        description = "다시 시도해주세요",
        isError = true,
        canRetryAnalysis = true,
    ),
)

private val DefaultNutrients = persistentListOf(
    NutrientProgressUiState(
        nutrientType = "CALORIE",
        label = "칼로리",
        percent = "75%",
        progress = 0.75f,
    ),
    NutrientProgressUiState(
        nutrientType = "CARBOHYDRATE",
        label = "탄수화물",
        percent = "58%",
        progress = 0.58f,
    ),
    NutrientProgressUiState(
        nutrientType = "PROTEIN",
        label = "단백질",
        percent = "40%",
        progress = 0.4f,
    ),
    NutrientProgressUiState(
        nutrientType = "FAT",
        label = "지방",
        percent = "52%",
        progress = 0.52f,
    ),
    NutrientProgressUiState(
        nutrientType = "SODIUM",
        label = "나트륨",
        percent = "65%",
        progress = 0.65f,
    ),
)
