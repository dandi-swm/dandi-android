package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.auth.entity.Gender
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyInlineNotice
import com.dandi.nyummy.common.presentation.component.NyummyInlineNoticeTone
import com.dandi.nyummy.common.presentation.component.NyummySegmentedControl
import com.dandi.nyummy.common.presentation.component.NyummyTextField
import com.dandi.nyummy.common.presentation.component.NyummyWheelPicker
import com.dandi.nyummy.common.presentation.component.NyummyWheelPickerFrame
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import java.util.Calendar
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 회원가입 3단계 — 프로필 입력 (Figma `MOB/LIVE/PROF-01`).
 *
 * 닉네임·성별·생년월일·키·몸무게를 입력받아 최종 가입을 실행한다.
 * 닉네임 검증 실패 시 안내 카드가 오류 배너로 바뀐다 (`PROF-01/ValidationError`).
 */
@Composable
internal fun SignUpProfileStep(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    Column {
        DandiText(
            text = stringResource(R.string.auth_signup_profile_title),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        Spacer(Modifier.height(SignUpTitleSubtitleSpacing))
        DandiText(
            text = stringResource(R.string.auth_signup_profile_subtitle),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularL,
        )
        Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space24))
        NyummyTextField(
            value = uiState.nickname,
            onValueChange = { onIntent(SignUpIntent.InputNickname(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.auth_signup_nickname_placeholder),
            label = stringResource(R.string.auth_signup_nickname_label),
            isError = uiState.nicknameError != null,
            enabled = !uiState.isLoading,
            leadingIcon = {
                Icon(
                    painter = painterResource(CommonR.drawable.nyummy_icon_user),
                    contentDescription = null,
                )
            },
        )
        Spacer(Modifier.height(SignUpProfileSectionSpacing))
        FieldLabel(text = stringResource(R.string.auth_signup_gender_label))
        NyummySegmentedControl(
            options = persistentListOf(
                stringResource(R.string.auth_signup_gender_male),
                stringResource(R.string.auth_signup_gender_female),
            ),
            selectedIndex = if (uiState.gender == Gender.MALE) 0 else 1,
            onSelect = { index ->
                onIntent(SignUpIntent.SelectGender(if (index == 0) Gender.MALE else Gender.FEMALE))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading,
        )
        Spacer(Modifier.height(SignUpProfileSectionSpacing))
        FieldLabel(text = stringResource(R.string.auth_signup_birth_label))
        BirthWheelPicker(uiState = uiState, onIntent = onIntent)
        Spacer(Modifier.height(SignUpProfileSectionSpacing))
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel(text = stringResource(R.string.auth_signup_height_label))
                NyummyWheelPickerFrame(modifier = Modifier.fillMaxWidth()) {
                    NyummyWheelPicker(
                        items = remember { HEIGHT_RANGE.map(Int::toString).toImmutableList() },
                        selectedIndex = uiState.height - HEIGHT_RANGE.first,
                        onSelectedIndexChange = {
                            onIntent(SignUpIntent.SelectHeight(HEIGHT_RANGE.first + it))
                        },
                        modifier = Modifier.weight(1f),
                        unitLabel = stringResource(R.string.auth_signup_unit_height),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel(text = stringResource(R.string.auth_signup_weight_label))
                NyummyWheelPickerFrame(modifier = Modifier.fillMaxWidth()) {
                    NyummyWheelPicker(
                        items = remember { WEIGHT_RANGE.map(Int::toString).toImmutableList() },
                        selectedIndex = uiState.weight - WEIGHT_RANGE.first,
                        onSelectedIndexChange = {
                            onIntent(SignUpIntent.SelectWeight(WEIGHT_RANGE.first + it))
                        },
                        modifier = Modifier.weight(1f),
                        unitLabel = stringResource(R.string.auth_signup_unit_weight),
                    )
                }
            }
        }
        Spacer(Modifier.height(SignUpProfileNoticeSpacing))
        if (uiState.nicknameError != null) {
            NyummyInlineNotice(
                title = stringResource(R.string.auth_signup_error_nickname_empty_title),
                modifier = Modifier.fillMaxWidth(),
                tone = NyummyInlineNoticeTone.Danger,
                description = stringResource(R.string.auth_signup_error_nickname_empty_desc),
            )
        } else {
            NyummyInlineNotice(
                title = stringResource(R.string.auth_signup_privacy_notice_title),
                modifier = Modifier.fillMaxWidth(),
                description = stringResource(R.string.auth_signup_privacy_notice_desc),
            )
        }
        Spacer(Modifier.height(SignUpProfileCtaSpacing))
        NyummyButton(
            label = stringResource(R.string.auth_signup_profile_cta),
            modifier = Modifier.fillMaxWidth(),
            style = NyummyButtonStyle.Primary,
            size = NyummyButtonSize.Large,
            enabled = !uiState.isLoading,
            loading = uiState.isLoading,
            onClick = { onIntent(SignUpIntent.ClickSubmit) },
        )
    }
}

@Composable
private fun BirthWheelPicker(
    uiState: SignUpUIState,
    onIntent: (SignUpIntent) -> Unit,
) {
    val years = remember { BIRTH_YEAR_RANGE.map(Int::toString).toImmutableList() }
    val months = remember { MONTH_RANGE.map(::toTwoDigits).toImmutableList() }
    val days = remember(uiState.birthYear, uiState.birthMonth) {
        (1..lengthOfMonth(uiState.birthYear, uiState.birthMonth))
            .map(::toTwoDigits)
            .toImmutableList()
    }
    NyummyWheelPickerFrame(modifier = Modifier.fillMaxWidth()) {
        NyummyWheelPicker(
            items = years,
            selectedIndex = uiState.birthYear - BIRTH_YEAR_RANGE.first,
            onSelectedIndexChange = {
                onIntent(SignUpIntent.SelectBirthYear(BIRTH_YEAR_RANGE.first + it))
            },
            modifier = Modifier.weight(1f),
        )
        NyummyWheelPicker(
            items = months,
            selectedIndex = uiState.birthMonth - MONTH_RANGE.first,
            onSelectedIndexChange = {
                onIntent(SignUpIntent.SelectBirthMonth(MONTH_RANGE.first + it))
            },
            modifier = Modifier.weight(1f),
        )
        NyummyWheelPicker(
            items = days,
            selectedIndex = uiState.birthDay - 1,
            onSelectedIndexChange = { onIntent(SignUpIntent.SelectBirthDay(it + 1)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    DandiText(
        text = text,
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        style = DesignSystemThemeImpl.typeScale.textStrongM,
    )
    Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space8))
}

private fun toTwoDigits(value: Int): String = "%02d".format(value)

private val BIRTH_YEAR_RANGE = 1900..Calendar.getInstance().get(Calendar.YEAR)
private val MONTH_RANGE = 1..12
private val HEIGHT_RANGE = 120..220
private val WEIGHT_RANGE = 30..150

private val SignUpProfileSectionSpacing = 28.dp
private val SignUpProfileNoticeSpacing = 30.dp
private val SignUpProfileCtaSpacing = 16.dp
