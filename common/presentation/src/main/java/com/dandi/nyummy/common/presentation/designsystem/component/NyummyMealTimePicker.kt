package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * 평소 식사 시각 드롭다운 한 줄. 왼쪽에 끼니, 오른쪽에 시각(안 먹으면 "안 먹어요")과 아래 화살표.
 * 누르면 [NyummyMealTimeSheet]를 연다.
 */
@Composable
fun NyummyMealTimeField(
    meal: Meal,
    time: MealTimeVO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = modifier
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
            text = stringResource(meal.nyummyLabelRes),
            style = NyummyTheme.typography.labelMStrong,
        )
        NyummyText(
            text = if (time.isSkipped) stringResource(R.string.nyummy_meal_skipped) else nyummyMealTimeText(time),
            style = NyummyTheme.typography.labelL,
            color = if (time.isSkipped) NyummyTheme.colors.content.tertiary else NyummyTheme.colors.content.primary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.nyummy_ic_chevron_down),
            contentDescription = null,
            tint = NyummyTheme.colors.content.tertiary,
            modifier = Modifier.size(NyummyTheme.size.iconM),
        )
    }
}

/**
 * 한 끼의 시각을 고르는 시트. 오전/오후, 시(1~12) 두 휠과 "안 먹어요", "확인". 정시 단위만 고른다.
 * 안 먹는다고 골라 둔 끼니를 다시 열면 남겨 둔 시각에서 시작한다.
 */
@Composable
fun NyummyMealTimeSheet(
    meal: Meal,
    initial: MealTimeVO,
    onSelect: (MealTimeVO) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mealLabel = stringResource(meal.nyummyLabelRes)
    var periodIndex by rememberSaveable(meal) { mutableIntStateOf(if (initial.hour >= 12) 1 else 0) }
    var hourIndex by rememberSaveable(meal) { mutableIntStateOf(hour12(initial.hour) - 1) }
    val periods = persistentListOf(stringResource(R.string.nyummy_meal_am), stringResource(R.string.nyummy_meal_pm))
    val hours = remember { (1..12).map(Int::toString).toImmutableList() }

    NyummyBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        NyummySheetTitle(text = stringResource(R.string.nyummy_meal_sheet_title, mealLabel))
        TimeWheels(
            periods = periods,
            hours = hours,
            periodIndex = periodIndex,
            hourIndex = hourIndex,
            onPeriodChange = { periodIndex = it },
            onHourChange = { hourIndex = it },
        )
        NyummyTextButton(
            text = stringResource(R.string.nyummy_meal_sheet_skip, mealLabel),
            onClick = { onSelect(initial.copy(isSkipped = true)) },
            tone = NyummyTextButtonTone.Neutral,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        NyummyButton(
            text = stringResource(R.string.nyummy_meal_sheet_confirm),
            onClick = {
                val hour24 = (hourIndex + 1) % 12 + if (periodIndex == 1) 12 else 0
                onSelect(MealTimeVO(hour = hour24))
            },
            size = NyummyButtonSize.L,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 두 휠을 나란히 두고 가운데 선택 밴드를 둘에 걸쳐 그린다(시트 바탕 위라 테두리는 없다). */
@Composable
private fun TimeWheels(
    periods: ImmutableList<String>,
    hours: ImmutableList<String>,
    periodIndex: Int,
    hourIndex: Int,
    onPeriodChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
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
            contentDescription = stringResource(R.string.nyummy_meal_period_description),
            modifier = Modifier.weight(1f),
        )
        NyummyWheelColumn(
            items = hours,
            selectedIndex = hourIndex,
            onSelectedIndexChange = onHourChange,
            unit = stringResource(R.string.nyummy_meal_unit_hour),
            contentDescription = stringResource(R.string.nyummy_meal_hour_description),
            modifier = Modifier.weight(1f),
        )
    }
}

/** "오전 8시", "오후 12시"처럼 읽는 시각. */
@Composable
fun nyummyMealTimeText(time: MealTimeVO): String = stringResource(
    R.string.nyummy_meal_time_format,
    stringResource(if (time.hour >= 12) R.string.nyummy_meal_pm else R.string.nyummy_meal_am),
    hour12(time.hour),
)

/** 끼니 이름(아침, 점심, 저녁). */
val Meal.nyummyLabelRes: Int
    get() = when (this) {
        Meal.BREAKFAST -> R.string.nyummy_meal_breakfast
        Meal.LUNCH -> R.string.nyummy_meal_lunch
        Meal.DINNER -> R.string.nyummy_meal_dinner
    }

/** 0~23시를 12시간제 1~12로 바꾼다(0시는 12, 13시는 1). */
private fun hour12(hour24: Int): Int = (hour24 + 11) % 12 + 1

private val MealRowHeight = 52.dp
private val WheelsHorizontalInset = 10.dp

