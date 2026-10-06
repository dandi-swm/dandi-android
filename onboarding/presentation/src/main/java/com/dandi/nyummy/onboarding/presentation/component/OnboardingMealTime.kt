package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimeVO
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelColumn
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelPickerFrame
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 대화창 안에 들어가는 평소 식사 시각 입력. 아침, 점심, 저녁 세 줄에 기본값을 미리 채워 두어 그대로 넘어갈 수 있다.
 * 줄을 누르면 시간 시트가 열리고, 맨 아래 버튼으로 마친다.
 */
@Composable
internal fun ColumnScope.OnboardingMealTimeSlot(
    mealTimes: MealTimesVO,
    enabled: Boolean,
    onClickMeal: (Meal) -> Unit,
    onDone: () -> Unit,
) {
    Meal.entries.forEach { meal ->
        MealTimeRow(
            meal = meal,
            time = mealTimes[meal],
            enabled = enabled,
            onClick = { onClickMeal(meal) },
        )
        Spacer(Modifier.height(NyummyTheme.spacing.s8))
    }
    Spacer(Modifier.height(NyummyTheme.spacing.s8))
    NyummyButton(
        text = stringResource(R.string.onboarding_meal_done),
        onClick = onDone,
        size = NyummyButtonSize.M,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 끼니 한 줄. 왼쪽에 끼니, 오른쪽에 시각(안 먹으면 "안 먹어요")과 아래 화살표. */
@Composable
private fun MealTimeRow(
    meal: Meal,
    time: MealTimeVO,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MealRowHeight)
            .nyummyClickable(onClick = onClick, enabled = enabled, interactionSource = interactionSource)
            .background(
                color = if (pressed) NyummyTheme.colors.bg.actionSecondaryPressed else NyummyTheme.colors.bg.surfaceSunken,
                shape = RoundedCornerShape(NyummyTheme.radius.s),
            )
            .padding(start = NyummyTheme.spacing.s16, end = NyummyTheme.spacing.s12),
        horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NyummyText(
            text = stringResource(meal.labelRes),
            style = NyummyTheme.typography.labelMStrong,
        )
        NyummyText(
            text = if (time.isSkipped) stringResource(R.string.onboarding_meal_skipped) else mealTimeText(time),
            style = NyummyTheme.typography.labelL,
            color = if (time.isSkipped) NyummyTheme.colors.content.tertiary else NyummyTheme.colors.content.primary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_chevron_down),
            contentDescription = null,
            tint = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.size(NyummyTheme.size.iconM),
        )
    }
}

/**
 * 한 끼의 시각을 고르는 시트. 오전/오후, 시(1~12), 분(10분 단위) 세 휠과 "안 먹어요", "확인".
 * 안 먹는다고 골라 둔 끼니를 다시 열면 남겨 둔 시각에서 시작한다.
 */
@Composable
internal fun OnboardingMealTimeSheet(
    meal: Meal,
    initial: MealTimeVO,
    onSelect: (MealTimeVO) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val mealLabel = stringResource(meal.labelRes)
    var periodIndex by rememberSaveable(meal) { mutableIntStateOf(if (initial.hour >= 12) 1 else 0) }
    var hourIndex by rememberSaveable(meal) { mutableIntStateOf(hour12(initial.hour) - 1) }
    var minuteIndex by rememberSaveable(meal) { mutableIntStateOf((initial.minute / MinuteStep).coerceIn(0, MinuteSlots - 1)) }
    val periods = persistentListOf(stringResource(R.string.onboarding_meal_am), stringResource(R.string.onboarding_meal_pm))
    val hours = remember { (1..12).map(Int::toString).toImmutableList() }
    val minutes = remember { (0 until MinuteSlots).map { String.format(Locale.US, "%02d", it * MinuteStep) }.toImmutableList() }

    NyummyBottomSheet(onDismissRequest = onDismissRequest) {
        NyummySheetTitle(text = stringResource(R.string.onboarding_meal_sheet_title, mealLabel))
        TimeWheels(
            periods = periods,
            hours = hours,
            minutes = minutes,
            periodIndex = periodIndex,
            hourIndex = hourIndex,
            minuteIndex = minuteIndex,
            onPeriodChange = { periodIndex = it },
            onHourChange = { hourIndex = it },
            onMinuteChange = { minuteIndex = it },
        )
        NyummyTextButton(
            text = stringResource(R.string.onboarding_meal_sheet_skip, mealLabel),
            onClick = { onSelect(initial.copy(isSkipped = true)) },
            tone = NyummyTextButtonTone.Neutral,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        NyummyButton(
            text = stringResource(R.string.onboarding_meal_sheet_confirm),
            onClick = {
                val hour24 = (hourIndex + 1) % 12 + if (periodIndex == 1) 12 else 0
                onSelect(MealTimeVO(hour = hour24, minute = minuteIndex * MinuteStep))
            },
            size = NyummyButtonSize.L,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 세 휠을 나란히 두고 가운데 선택 밴드를 셋에 걸쳐 그린다(시트 바탕 위라 테두리는 없다). */
@Composable
private fun TimeWheels(
    periods: ImmutableList<String>,
    hours: ImmutableList<String>,
    minutes: ImmutableList<String>,
    periodIndex: Int,
    hourIndex: Int,
    minuteIndex: Int,
    onPeriodChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    NyummyWheelPickerFrame(
        framed = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WheelsHorizontalInset),
    ) {
        NyummyWheelColumn(
            items = periods,
            selectedIndex = periodIndex,
            onSelectedIndexChange = onPeriodChange,
            contentDescription = stringResource(R.string.onboarding_meal_period_description),
            modifier = Modifier.weight(1f),
        )
        NyummyWheelColumn(
            items = hours,
            selectedIndex = hourIndex,
            onSelectedIndexChange = onHourChange,
            unit = stringResource(R.string.onboarding_meal_unit_hour),
            contentDescription = stringResource(R.string.onboarding_meal_hour_description),
            modifier = Modifier.weight(1f),
        )
        NyummyWheelColumn(
            items = minutes,
            selectedIndex = minuteIndex,
            onSelectedIndexChange = onMinuteChange,
            unit = stringResource(R.string.onboarding_meal_unit_minute),
            contentDescription = stringResource(R.string.onboarding_meal_minute_description),
            modifier = Modifier.weight(1f),
        )
    }
}

/** "오전 8:00", "오후 12:30"처럼 읽는 시각. */
@Composable
internal fun mealTimeText(time: MealTimeVO): String = stringResource(
    R.string.onboarding_meal_time_format,
    stringResource(if (time.hour >= 12) R.string.onboarding_meal_pm else R.string.onboarding_meal_am),
    hour12(time.hour),
    time.minute,
)

internal val Meal.labelRes: Int
    get() = when (this) {
        Meal.BREAKFAST -> R.string.onboarding_meal_breakfast
        Meal.LUNCH -> R.string.onboarding_meal_lunch
        Meal.DINNER -> R.string.onboarding_meal_dinner
    }

/** 0~23시를 12시간제 1~12로 바꾼다(0시는 12, 13시는 1). */
private fun hour12(hour24: Int): Int = (hour24 + 11) % 12 + 1

private const val MinuteStep = 10
private const val MinuteSlots = 60 / MinuteStep
private val MealRowHeight = 52.dp
private val WheelsHorizontalInset = 10.dp

