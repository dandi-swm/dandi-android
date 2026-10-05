package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyTextField
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.onboarding.domain.CatNameError
import com.dandi.nyummy.onboarding.domain.CatNameValidator
import com.dandi.nyummy.onboarding.presentation.OnboardingChoice
import com.dandi.nyummy.onboarding.presentation.R
import kotlinx.collections.immutable.ImmutableList

/** 선택지. 대사창 바로 위에 버튼으로 쌓는다. */
@Composable
internal fun OnboardingChoiceList(
    options: ImmutableList<OnboardingChoice>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
    ) {
        options.forEachIndexed { index, option ->
            NyummyButton(
                label = stringResource(option.labelRes),
                modifier = Modifier.fillMaxWidth(),
                style = NyummyButtonStyle.Secondary,
                size = NyummyButtonSize.Large,
                onClick = { onSelect(index) },
            )
        }
    }
}

/** 고양이 이름 입력 카드. */
@Composable
internal fun OnboardingCatNamePanel(
    value: String,
    error: CatNameError?,
    isSubmitting: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
        color = colors.bgSurfaceIvory,
        border = BorderStroke(1.dp, colors.borderCoachBubble),
    ) {
        Column(
            modifier = Modifier.padding(spacing.space16),
            verticalArrangement = Arrangement.spacedBy(spacing.space12),
        ) {
            NyummyTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.onboarding_name_placeholder),
                label = stringResource(R.string.onboarding_name_label),
                helperText = when (error) {
                    CatNameError.EMPTY -> stringResource(R.string.onboarding_name_error_empty)
                    CatNameError.TOO_LONG -> stringResource(R.string.onboarding_name_error_too_long, CatNameValidator.MAX_LENGTH)
                    null -> stringResource(R.string.onboarding_name_helper, CatNameValidator.MAX_LENGTH)
                },
                isError = error != null,
                enabled = !isSubmitting,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )
            NyummyButton(
                label = stringResource(R.string.onboarding_name_submit),
                modifier = Modifier.fillMaxWidth(),
                size = NyummyButtonSize.Large,
                loading = isSubmitting,
                enabled = !isSubmitting,
                onClick = onSubmit,
            )
        }
    }
}

/** 마지막 "함께 시작하기" 버튼. 이름 입력 버튼과 같은 기본(초록) 스타일로 이어지게 한다. */
@Composable
internal fun OnboardingStartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NyummyButton(
        label = stringResource(R.string.onboarding_start),
        modifier = modifier.fillMaxWidth(),
        style = NyummyButtonStyle.Primary,
        size = NyummyButtonSize.Large,
        onClick = onClick,
    )
}
