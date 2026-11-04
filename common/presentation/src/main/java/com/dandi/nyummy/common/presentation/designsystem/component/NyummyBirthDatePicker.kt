package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.toImmutableList
import java.time.YearMonth

/**
 * 생년월일. 연, 월, 일을 각각 작은 휠 피커([NyummyWheelPicker] compact)로 나란히 둔다(간격 8).
 * 회원가입과 마이페이지가 같은 모양을 쓴다.
 *
 * 일 목록은 고른 연·월의 마지막 날까지다. 고른 일이 그달에 없으면(2월 30일 등) 마지막 날로 보여 주고,
 * 그 값을 저장할지는 쓰는 곳이 [birthDayOfMonth]로 맞춘다.
 *
 * @param years 고를 수 있는 해. 끝은 보통 올해(기기 타임존)다.
 */
@Composable
fun NyummyBirthDatePicker(
    year: Int,
    month: Int,
    day: Int,
    years: IntRange,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val yearItems = remember(years) { years.map(Int::toString).toImmutableList() }
    val monthItems = remember { MONTHS.map(::toTwoDigits).toImmutableList() }
    val lastDay = birthDayOfMonth(year, month, Int.MAX_VALUE)
    val dayItems = remember(lastDay) { (1..lastDay).map(::toTwoDigits).toImmutableList() }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8)) {
        NyummyWheelPicker(
            items = yearItems,
            selectedIndex = year.coerceIn(years) - years.first,
            onSelectedIndexChange = { onYearChange(years.first + it) },
            unit = stringResource(R.string.nyummy_birth_unit_year),
            contentDescription = stringResource(R.string.nyummy_birth_year_description),
            compact = true,
            modifier = Modifier.weight(1f),
        )
        NyummyWheelPicker(
            items = monthItems,
            selectedIndex = month.coerceIn(MONTHS) - MONTHS.first,
            onSelectedIndexChange = { onMonthChange(MONTHS.first + it) },
            unit = stringResource(R.string.nyummy_birth_unit_month),
            contentDescription = stringResource(R.string.nyummy_birth_month_description),
            compact = true,
            modifier = Modifier.weight(1f),
        )
        NyummyWheelPicker(
            items = dayItems,
            selectedIndex = day.coerceIn(1, lastDay) - 1,
            onSelectedIndexChange = { onDayChange(it + 1) },
            unit = stringResource(R.string.nyummy_birth_unit_day),
            contentDescription = stringResource(R.string.nyummy_birth_day_description),
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** [day]를 [year]년 [month]월의 마지막 날 안으로 맞춘 값. 월을 바꿔 고른 일이 사라졌을 때 쓴다. */
fun birthDayOfMonth(year: Int, month: Int, day: Int): Int =
    day.coerceIn(1, YearMonth.of(year, month.coerceIn(MONTHS)).lengthOfMonth())

private fun toTwoDigits(value: Int): String = "%02d".format(value)

private val MONTHS = 1..12

@Preview(showBackground = true, widthDp = 350)
@Composable
private fun NyummyBirthDatePickerPreview() {
    NyummyTheme {
        NyummyBirthDatePicker(
            year = 2001,
            month = 3,
            day = 14,
            years = 1900..2026,
            onYearChange = {},
            onMonthChange = {},
            onDayChange = {},
        )
    }
}
