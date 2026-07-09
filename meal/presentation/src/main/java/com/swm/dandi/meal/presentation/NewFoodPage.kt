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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swm.dandi.common.presentation.component.ArchiText
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl

@Composable
fun NewFoodPage(
    viewModel: NewFoodViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NewFoodPageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun NewFoodPageContent(
    uiState: NewFoodUIState,
    onIntent: (NewFoodIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isAnalyzing) {
        NutritionAnalysisContent(
            uiState = uiState,
            onDismiss = { onIntent(NewFoodIntent.DismissAnalysis) },
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        MealRecordTopBar(
            title = "새 식사 기록",
            onBack = { onIntent(NewFoodIntent.ClickBack) },
        )
        Spacer(modifier = Modifier.height(36.dp))
        FoodNameSection(uiState = uiState, onIntent = onIntent)
        Spacer(modifier = Modifier.height(28.dp))
        EatenTimeCard(uiState = uiState, onIntent = onIntent)
        Spacer(modifier = Modifier.height(28.dp))
        PhotoSection(uiState = uiState, onIntent = onIntent)
        Spacer(modifier = Modifier.height(38.dp))
        SubmitButton(uiState = uiState, onSubmit = { onIntent(NewFoodIntent.Submit) })
        if (uiState.submitResultMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            StatusMessage(text = uiState.submitResultMessage)
        }
    }
}

@Composable
private fun MealRecordTopBar(
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
private fun FoodNameSection(
    uiState: NewFoodUIState,
    onIntent: (NewFoodIntent) -> Unit,
) {
    FormSection(title = "음식 이름") {
        FormTextField(
            value = uiState.foodName,
            hint = "예 : 닭가슴살 샐러드",
            isError = uiState.foodNameError.isNotEmpty(),
            onValueChange = { onIntent(NewFoodIntent.ChangeFoodName(it)) },
        )
        ErrorText(text = uiState.foodNameError)
    }
}

@Composable
private fun EatenTimeCard(
    uiState: NewFoodUIState,
    onIntent: (NewFoodIntent) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        border = BorderStroke(
            width = 1.dp,
            color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArchiText(
                        text = "먹은 시간",
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                    )
                    SessionPill(text = uiState.mealSessionLabel)
                }
                TimeBox(text = uiState.eatenTimeLabel.ifEmpty { "시간 선택" })
            }
            TimeOptionRow(
                options = listOf(
                    TimeOption("지금", null, null),
                    TimeOption("08:00", 8, 0),
                    TimeOption("12:30", 12, 30),
                    TimeOption("18:30", 18, 30),
                ),
                selectedTimeLabel = uiState.eatenTimeLabel,
                onIntent = onIntent,
            )
            ErrorText(text = uiState.eatenTimeError)
        }
    }
}

@Composable
private fun SessionPill(text: String) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DesignSystemThemeImpl.designSystemColor.contentAccent.copy(alpha = 0.72f))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = text,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
            maxLines = 1,
        )
    }
}

@Composable
private fun TimeBox(text: String) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2)
            .border(
                width = 1.dp,
                color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
                shape = RoundedCornerShape(8.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = text,
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
            maxLines = 1,
        )
    }
}

@Composable
private fun PhotoSection(
    uiState: NewFoodUIState,
    onIntent: (NewFoodIntent) -> Unit,
) {
    FormSection(
        title = "음식 사진",
        trailingText = if (uiState.photoError.isNotEmpty()) uiState.photoError else "",
    ) {
        PhotoDropZone(
            isPhotoAttached = uiState.isPhotoAttached,
            isError = uiState.photoError.isNotEmpty(),
            onClick = { onIntent(NewFoodIntent.TogglePhotoAttachment) },
        )
    }
}

@Composable
private fun PhotoDropZone(
    isPhotoAttached: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val accentWash = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2.copy(alpha = 0.72f)
    val borderColor = if (isError) {
        DesignSystemThemeImpl.designSystemColor.contentFavorite
    } else {
        DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp)
            .clip(shape)
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
                border = BorderStroke(
                    width = 1.dp,
                    color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel1,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = DesignSystemThemeImpl.designSystemColor.contentAccent,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(34.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0.copy(alpha = 0.86f))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                ArchiText(
                    text = if (isPhotoAttached) "사진이 추가되었어요" else "터치하여 사진 추가",
                    style = DesignSystemThemeImpl.typeScale.textRegularM,
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SubmitButton(
    uiState: NewFoodUIState,
    onSubmit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                if (uiState.isReadyToSubmit) {
                    DesignSystemThemeImpl.designSystemColor.contentAccent
                } else {
                    DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2
                }
            )
            .clickable(enabled = uiState.isReadyToSubmit, onClick = onSubmit),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = "기록 저장하기",
            style = DesignSystemThemeImpl.typeScale.textStrongL,
            color = if (uiState.isReadyToSubmit) {
                DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1
            } else {
                DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2
            },
            maxLines = 1,
        )
    }
}

@Composable
private fun NutritionAnalysisContent(
    uiState: NewFoodUIState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.14f))
        AnalysisIllustration()
        Spacer(modifier = Modifier.height(46.dp))
        ArchiText(
            text = uiState.analysisTitle,
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(22.dp))
        ArchiText(
            text = uiState.analysisDescription,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            textAlign = TextAlign.Center,
        )
        ArchiText(
            text = uiState.analysisSubDescription,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(42.dp))
        LinearProgressIndicator(
            progress = { uiState.analysisProgress },
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(14.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = DesignSystemThemeImpl.designSystemColor.contentAccent.copy(alpha = 0.78f),
            trackColor = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel2,
        )
        Spacer(modifier = Modifier.weight(1f))
        LaterButton(onClick = onDismiss)
        Spacer(modifier = Modifier.height(68.dp))
    }
}

@Composable
private fun AnalysisIllustration() {
    // TODO: Lottie asset이 준비되면 실제 애니메이션 컴포저블로 교체한다.
    Box(
        modifier = Modifier.size(186.dp),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = "Lottie 애니메이션",
            style = DesignSystemThemeImpl.typeScale.textStrongM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            maxLines = 1,
        )
    }
}

@Composable
private fun LaterButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            modifier = Modifier.size(24.dp),
        )
        ArchiText(
            text = "나중에 확인하기",
            style = DesignSystemThemeImpl.typeScale.textRegularM,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            maxLines = 1,
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    trailingText: String = "",
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchiText(
                text = title,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                maxLines = 1,
            )
            if (trailingText.isNotEmpty()) {
                ArchiText(
                    text = trailingText,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                    color = DesignSystemThemeImpl.designSystemColor.contentFavorite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        content()
    }
}

@Composable
private fun FormTextField(
    value: String,
    hint: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val textStyle = DesignSystemThemeImpl.typeScale.textRegularL.copy(
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0)
            .border(
                width = 1.dp,
                color = if (isError) {
                    DesignSystemThemeImpl.designSystemColor.contentFavorite
                } else {
                    DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3
                },
            )
            .padding(horizontal = 10.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(textStyle),
        cursorBrush = SolidColor(DesignSystemThemeImpl.designSystemColor.contentAccent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    ArchiText(
                        text = hint,
                        style = DesignSystemThemeImpl.typeScale.textRegularL,
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun TimeOptionRow(
    options: List<TimeOption>,
    selectedTimeLabel: String,
    onIntent: (NewFoodIntent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option.timeLabel == selectedTimeLabel
            TimeChip(
                text = option.label,
                selected = isSelected,
                modifier = Modifier.weight(1f),
                onClick = {
                    if (option.hour == null || option.minute == null) {
                        onIntent(NewFoodIntent.SelectCurrentTime)
                    } else {
                        onIntent(NewFoodIntent.SelectEatenTime(option.hour, option.minute))
                    }
                },
            )
        }
    }
}

@Composable
private fun TimeChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
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
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ArchiText(
            text = text,
            style = DesignSystemThemeImpl.typeScale.textRegularM,
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
private fun ErrorText(text: String) {
    if (text.isEmpty()) return
    ArchiText(
        text = text,
        style = DesignSystemThemeImpl.typeScale.textRegularS,
        color = DesignSystemThemeImpl.designSystemColor.contentFavorite,
        maxLines = 2,
    )
}

@Composable
private fun StatusMessage(text: String) {
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

private data class TimeOption(
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
private fun NewFoodPagePreview() {
    DesignSystemTheme {
        NewFoodPageContent(
            uiState = NewFoodUIState(
                foodName = "닭가슴살 샐러드",
                isPhotoAttached = true,
                eatenHour = 12,
                eatenMinute = 30,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NutritionAnalysisPreview() {
    DesignSystemTheme {
        NutritionAnalysisContent(
            uiState = NewFoodUIState(isAnalyzing = true),
            onDismiss = {},
        )
    }
}
