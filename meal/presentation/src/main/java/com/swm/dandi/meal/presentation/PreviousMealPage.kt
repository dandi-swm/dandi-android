package com.swm.dandi.meal.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swm.dandi.common.presentation.R
import com.swm.dandi.common.presentation.component.ArchiText
import com.swm.dandi.common.presentation.imageUtil.UrlImage
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl
import kotlinx.collections.immutable.persistentListOf

private val DefaultPreviousFoods = persistentListOf(
    PreviousFoodUiState(
        id = "rice",
        name = "흰 쌀밥",
        iconImageUrl = "dandi://drawable/meal_ic_food_rice_pixel",
        recordCountLabel = "42회 기록",
        lastRecordedLabel = "마지막: 어제 점심",
    ),
    PreviousFoodUiState(
        id = "sandwich",
        name = "햄치즈 샌드위치",
        iconImageUrl = "dandi://drawable/meal_ic_food_sandwich_pixel",
        recordCountLabel = "18회 기록",
        lastRecordedLabel = "마지막: 3일 전 아침",
    ),
    PreviousFoodUiState(
        id = "salad",
        name = "닭가슴살 샐러드",
        iconImageUrl = "dandi://drawable/meal_ic_food_salad_pixel",
        recordCountLabel = "30회 기록",
        lastRecordedLabel = "마지막: 1주일 전 저녁",
    ),
    PreviousFoodUiState(
        id = "kimbap",
        name = "김밥",
        iconImageUrl = "dandi://drawable/meal_ic_food_kimbap_pixel",
        recordCountLabel = "5회 기록",
        lastRecordedLabel = "마지막: 2주일 전 점심",
    ),
)

@Composable
fun PreviousMealPage(
    viewModel: PreviousMealViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PreviousMealPageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun PreviousMealPageContent(
    uiState: PreviousMealUIState,
    onIntent: (PreviousMealIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        PreviousMealTopBar(
            title = "이전 식사 기록",
            onBack = { onIntent(PreviousMealIntent.ClickBack) },
        )
        Spacer(modifier = Modifier.height(54.dp))
        ArchiText(
            text = "자주 먹는 식사를 빠르게 기록하세요.",
            modifier = Modifier.fillMaxWidth(),
            style = DesignSystemThemeImpl.typeScale.textRegularL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(16.dp))
        RecentHeader()
        Spacer(modifier = Modifier.height(24.dp))
        PreviousMealTimeControl(uiState = uiState, onIntent = onIntent)
        Spacer(modifier = Modifier.height(24.dp))
        PreviousFoodList(uiState = uiState, onIntent = onIntent)
        if (uiState.selectedFoodError.isNotEmpty() || uiState.eatenTimeError.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            PreviousMealError(uiState = uiState)
        }
        if (uiState.submitResultMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            PreviousMealStatus(text = uiState.submitResultMessage)
        }
        Spacer(modifier = Modifier.height(28.dp))
        PreviousMealSubmitButton(
            enabled = !uiState.isSubmitting && uiState.selectedFoodId.isNotEmpty() && uiState.hasEatenTime,
            onClick = { onIntent(PreviousMealIntent.Submit) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        NewFoodShortcutButton(onClick = { onIntent(PreviousMealIntent.ClickNewFood) })
    }
}

@Composable
private fun PreviousMealTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp),
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로 가기",
                tint = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                modifier = Modifier.size(28.dp),
            )
        }
        ArchiText(
            text = title,
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecentHeader() {
    ArchiText(
        text = "최근 기록된 항목들",
        modifier = Modifier.fillMaxWidth(),
        style = DesignSystemThemeImpl.typeScale.textRegularM,
        color = DesignSystemThemeImpl.designSystemColor.contentAccent,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun PreviousMealTimeControl(
    uiState: PreviousMealUIState,
    onIntent: (PreviousMealIntent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        border = BorderStroke(
            width = 1.dp,
            color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ArchiText(
                        text = "먹은 시간",
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                    )
                    ArchiText(
                        text = uiState.mealSessionLabel,
                        style = DesignSystemThemeImpl.typeScale.textRegularS,
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                    )
                }
                ArchiText(
                    text = uiState.eatenTimeLabel.ifEmpty {
                        stringResource(R.string.previous_meal_time_not_selected)
                    },
                    style = DesignSystemThemeImpl.typeScale.textStrongL,
                    color = DesignSystemThemeImpl.designSystemColor.contentAccent,
                    maxLines = 1,
                )
            }
            PreviousTimeOptionRow(
                options = listOf(
                    PreviousTimeOption(stringResource(R.string.previous_meal_time_now), null, null),
                    PreviousTimeOption("08:00", 8, 0),
                    PreviousTimeOption("12:30", 12, 30),
                    PreviousTimeOption("18:30", 18, 30),
                ),
                selectedTimeLabel = uiState.eatenTimeLabel,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun PreviousTimeOptionRow(
    options: List<PreviousTimeOption>,
    selectedTimeLabel: String,
    onIntent: (PreviousMealIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val selected = option.timeLabel == selectedTimeLabel
            PreviousTimeChip(
                text = option.label,
                selected = selected,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (option.hour == null || option.minute == null) {
                        onIntent(PreviousMealIntent.SelectCurrentTime)
                    } else {
                        onIntent(PreviousMealIntent.SelectEatenTime(option.hour, option.minute))
                    }
                },
            )
        }
    }
}

@Composable
private fun PreviousTimeChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(
                if (selected) {
                    DesignSystemThemeImpl.designSystemColor.contentAccent
                } else {
                    DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    DesignSystemThemeImpl.designSystemColor.contentAccent
                } else {
                    DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
                },
                shape = RoundedCornerShape(17.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = text,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
            color = if (selected) {
                DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
            } else {
                DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun PreviousFoodList(
    uiState: PreviousMealUIState,
    onIntent: (PreviousMealIntent) -> Unit,
) {
    when {
        uiState.isLoading -> {
            PreviousMealListStatus(text = "이전 음식 목록을 불러오는 중입니다.")
            return
        }

        uiState.loadErrorMessage.isNotEmpty() -> {
            PreviousMealListStatus(text = uiState.loadErrorMessage)
            return
        }

        uiState.foods.isEmpty() -> {
            PreviousMealListStatus(text = "아직 기록된 이전 음식이 없어요.")
            return
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        uiState.foods.forEach { food ->
            PreviousFoodItem(
                food = food,
                selected = food.id == uiState.selectedFoodId,
                onSelect = { onIntent(PreviousMealIntent.SelectFood(food.id)) },
                onQuickRecord = { onIntent(PreviousMealIntent.QuickRecord(food.id)) },
            )
        }
    }
}

@Composable
private fun PreviousMealListStatus(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        border = BorderStroke(
            width = 1.dp,
            color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
        ),
    ) {
        ArchiText(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            maxLines = 2,
        )
    }
}

@Composable
private fun PreviousFoodItem(
    food: PreviousFoodUiState,
    selected: Boolean,
    onSelect: () -> Unit,
    onQuickRecord: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .background(
                if (selected) {
                    DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
                } else {
                    DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0
                }
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = DesignSystemThemeImpl.designSystemColor.contentAccent,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodThumbnail(
            imageUrl = food.iconImageUrl,
            contentDescription = "${food.name} 음식 아이콘",
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ArchiText(
                text = food.name,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ArchiText(
                text = food.lastRecordedLabel,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CountBadge(text = food.recordCountLabel)
        Spacer(modifier = Modifier.width(10.dp))
        QuickAddButton(onClick = onQuickRecord)
    }
}

@Composable
private fun FoodThumbnail(
    imageUrl: String,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2)
            .border(
                width = 1.dp,
                color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        UrlImage(
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentScale = ContentScale.Fit,
        ) {
            FoodFallbackIcon()
        }
    }
}

@Composable
private fun FoodFallbackIcon() {
    Icon(
        imageVector = Icons.Filled.Restaurant,
        contentDescription = null,
        tint = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        modifier = Modifier.size(34.dp),
    )
}

@Composable
private fun CountBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = text,
            style = DesignSystemThemeImpl.typeScale.textRegularXS,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = DesignSystemThemeImpl.designSystemColor.contentAccent.copy(alpha = 0.72f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = DesignSystemThemeImpl.designSystemColor.contentAccent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PreviousMealSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(
                if (enabled) {
                    DesignSystemThemeImpl.designSystemColor.contentAccent
                } else {
                    DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = "선택한 음식 기록하기",
            style = DesignSystemThemeImpl.typeScale.textStrongL,
            color = if (enabled) {
                DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
            } else {
                DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun NewFoodShortcutButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
                shape = RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = "새 식사 기록",
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
            maxLines = 1,
        )
    }
}

@Composable
private fun PreviousMealError(uiState: PreviousMealUIState) {
    val text = listOf(uiState.selectedFoodError, uiState.eatenTimeError)
        .filter { it.isNotEmpty() }
        .joinToString(separator = " ")
    ArchiText(
        text = text,
        style = DesignSystemThemeImpl.typeScale.textRegularS,
        color = DesignSystemThemeImpl.designSystemColor.contentFavorite,
        maxLines = 2,
    )
}

@Composable
private fun PreviousMealStatus(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        border = BorderStroke(
            width = 1.dp,
            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
        ),
    ) {
        ArchiText(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
            maxLines = 2,
        )
    }
}

private data class PreviousTimeOption(
    val label: String,
    val hour: Int?,
    val minute: Int?,
) {
    val timeLabel: String
        get() = if (hour == null || minute == null) {
            ""
        } else {
            "${hour.toTwoDigits()}:${minute.toTwoDigits()}"
        }
}

private fun Int.toTwoDigits(): String = toString().padStart(length = 2, padChar = '0')

@Preview(showBackground = true)
@Composable
private fun PreviousMealPagePreview() {
    DesignSystemTheme {
        PreviousMealPageContent(
            uiState = PreviousMealUIState(
                foods = DefaultPreviousFoods,
                isLoading = false,
                selectedFoodId = "rice",
            ),
            onIntent = {},
        )
    }
}
