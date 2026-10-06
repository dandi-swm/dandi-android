package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNotice
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNoticeTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySegmentedControl
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyWheelPicker
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 회원가입 3단계. 집사 이름, 성별, 생년월일, 키, 몸무게.
 * 이름 검증에 실패하면 아래 안내 카드가 같은 자리에서 오류(Danger)로 바뀐다.
 */
@Composable
internal fun ColumnScope.SignUpProfileStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val theme = NyummyTheme
    val enabled = !uiState.isLoading
    AuthFormHeader(
        title = stringResource(R.string.auth_signup_profile_title),
        subtitle = stringResource(R.string.auth_signup_profile_subtitle),
    )
    NyummyTextField(
        value = uiState.nickname,
        onValueChange = { onIntent(SignUpIntent.InputNickname(it)) },
        label = stringResource(R.string.auth_signup_nickname_label),
        placeholder = stringResource(R.string.auth_signup_nickname_placeholder),
        errorMessage = uiState.nicknameError?.let { stringResource(it.messageRes()) },
        leadingIcon = CommonR.drawable.nyummy_ic_user_round,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
    SectionGap()
    FieldLabel(stringResource(R.string.auth_signup_gender_label))
    NyummySegmentedControl(
        options = persistentListOf(
            stringResource(R.string.auth_signup_gender_male),
            stringResource(R.string.auth_signup_gender_female),
        ),
        selectedIndex = if (uiState.gender == Gender.MALE) 0 else 1,
        onSelect = { index -> onIntent(SignUpIntent.SelectGender(if (index == 0) Gender.MALE else Gender.FEMALE)) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
    SectionGap()
    FieldLabel(stringResource(R.string.auth_signup_birth_label))
    BirthWheelPickers(uiState = uiState, onIntent = onIntent)
    SectionGap()
    Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.s12)) {
        Column(modifier = Modifier.weight(1f)) {
            FieldLabel(stringResource(R.string.auth_signup_height_label))
            NyummyWheelPicker(
                items = remember { HEIGHT_RANGE.map(Int::toString).toImmutableList() },
                selectedIndex = uiState.height - HEIGHT_RANGE.first,
                onSelectedIndexChange = { onIntent(SignUpIntent.SelectHeight(HEIGHT_RANGE.first + it)) },
                unit = stringResource(R.string.auth_signup_unit_height),
                contentDescription = stringResource(R.string.auth_signup_height_label),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            FieldLabel(stringResource(R.string.auth_signup_weight_label))
            NyummyWheelPicker(
                items = remember { WEIGHT_RANGE.map(Int::toString).toImmutableList() },
                selectedIndex = uiState.weight - WEIGHT_RANGE.first,
                onSelectedIndexChange = { onIntent(SignUpIntent.SelectWeight(WEIGHT_RANGE.first + it)) },
                unit = stringResource(R.string.auth_signup_unit_weight),
                contentDescription = stringResource(R.string.auth_signup_weight_label),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    SectionGap()
    if (uiState.nicknameError != null) {
        NyummyInlineNotice(
            title = stringResource(R.string.auth_signup_error_nickname_empty_title),
            body = stringResource(R.string.auth_signup_error_nickname_empty_desc),
            tone = NyummyInlineNoticeTone.Danger,
        )
    } else {
        NyummyInlineNotice(
            title = stringResource(R.string.auth_signup_privacy_notice_title),
            body = stringResource(R.string.auth_signup_privacy_notice_desc),
        )
    }
}

/** 3단계 하단 고정 영역(위에 1px 구분선). "냐미 시작하기". */
@Composable
internal fun SignUpProfileBottom(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    NyummyButton(
        text = stringResource(R.string.auth_signup_profile_cta),
        onClick = { onIntent(SignUpIntent.ClickSubmit) },
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 생년월일은 연, 월, 일을 각각 독립된 휠 피커로 나란히 둔다(간격 8). */
@Composable
private fun BirthWheelPickers(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val theme = NyummyTheme
    val years = remember { BIRTH_YEAR_RANGE.map(Int::toString).toImmutableList() }
    val months = remember { MONTH_RANGE.map(::toTwoDigits).toImmutableList() }
    val days = remember(uiState.birthYear, uiState.birthMonth) {
        (1..lengthOfMonth(uiState.birthYear, uiState.birthMonth)).map(::toTwoDigits).toImmutableList()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.s8)) {
        NyummyWheelPicker(
            items = years,
            selectedIndex = uiState.birthYear - BIRTH_YEAR_RANGE.first,
            onSelectedIndexChange = { onIntent(SignUpIntent.SelectBirthYear(BIRTH_YEAR_RANGE.first + it)) },
            unit = stringResource(R.string.auth_signup_unit_year),
            contentDescription = stringResource(R.string.auth_signup_birth_year_description),
            modifier = Modifier.weight(1f),
        )
        NyummyWheelPicker(
            items = months,
            selectedIndex = uiState.birthMonth - MONTH_RANGE.first,
            onSelectedIndexChange = { onIntent(SignUpIntent.SelectBirthMonth(MONTH_RANGE.first + it)) },
            unit = stringResource(R.string.auth_signup_unit_month),
            contentDescription = stringResource(R.string.auth_signup_birth_month_description),
            modifier = Modifier.weight(1f),
        )
        NyummyWheelPicker(
            items = days,
            selectedIndex = uiState.birthDay - 1,
            onSelectedIndexChange = { onIntent(SignUpIntent.SelectBirthDay(it + 1)) },
            unit = stringResource(R.string.auth_signup_unit_day),
            contentDescription = stringResource(R.string.auth_signup_birth_day_description),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    NyummyText(text = text, style = NyummyTheme.typography.titleS)
    Spacer(Modifier.height(NyummyTheme.spacing.s8))
}

@Composable
private fun SectionGap() {
    Spacer(Modifier.height(NyummyTheme.spacing.s24))
}

private fun toTwoDigits(value: Int): String = "%02d".format(value)

// 기기 타임존이 아니라 한국 기준 올해까지. 연말 자정 근처에서 한 해가 빠지지 않게 한다.
private val BIRTH_YEAR_RANGE = 1900..KstTime.now().year
private val MONTH_RANGE = 1..12
private val HEIGHT_RANGE = 120..220
private val WEIGHT_RANGE = 30..150

