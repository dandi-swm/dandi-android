package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextField
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.domain.CatNameError
import com.dandi.nyummy.onboarding.domain.CatNameValidator
import com.dandi.nyummy.onboarding.presentation.OnboardingChoice
import com.dandi.nyummy.onboarding.presentation.R
import kotlinx.collections.immutable.ImmutableList

/** 선택지. 대화창 아래에 보조 버튼(L)으로 쌓는다. */
@Composable
internal fun OnboardingChoiceList(
    options: ImmutableList<OnboardingChoice>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
    ) {
        options.forEachIndexed { index, option ->
            NyummyButton(
                text = stringResource(option.labelRes),
                onClick = { onSelect(index) },
                style = NyummyButtonStyle.Secondary,
                size = NyummyButtonSize.L,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 이름을 받은 뒤 홈으로 넘어가는 마지막 버튼. */
@Composable
internal fun OnboardingStartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    NyummyButton(
        text = stringResource(R.string.onboarding_start),
        onClick = onClick,
        size = NyummyButtonSize.L,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * 대화창 안에 들어가는 고양이 이름 입력. 등록 중에는 입력과 버튼을 막고 버튼 문구를 "등록 중…"으로 바꾼다.
 * 오류는 입력칸 아래에 원인과 방법으로 보여 준다.
 */
@Composable
internal fun ColumnScope.OnboardingCatNameSlot(
    value: String,
    error: CatNameError?,
    isSubmitting: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    NyummyTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.onboarding_name_label),
        placeholder = stringResource(R.string.onboarding_name_placeholder),
        helperText = stringResource(R.string.onboarding_name_helper, CatNameValidator.MAX_LENGTH),
        errorMessage = when (error) {
            CatNameError.EMPTY -> stringResource(R.string.onboarding_name_error_empty)
            CatNameError.TOO_LONG -> stringResource(R.string.onboarding_name_error_too_long, CatNameValidator.MAX_LENGTH)
            null -> null
        },
        enabled = !isSubmitting,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(SlotGap))
    NyummyButton(
        text = stringResource(if (isSubmitting) R.string.onboarding_name_submitting else R.string.onboarding_name_submit),
        onClick = onSubmit,
        size = NyummyButtonSize.M,
        enabled = !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val SlotGap = 10.dp
