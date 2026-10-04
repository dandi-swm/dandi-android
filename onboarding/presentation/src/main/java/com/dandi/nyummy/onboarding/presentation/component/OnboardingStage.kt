package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.onboarding.presentation.OnboardingCharacter
import com.dandi.nyummy.onboarding.presentation.R

private const val CharacterWidthFraction = 0.78f
private const val CharacterCenterYFraction = -0.12f
private const val SilhouetteWidthFraction = 0.62f
private const val SilhouetteXFraction = -0.12f
private const val SilhouetteYFraction = -0.17f
private const val BottomScrimHeightFraction = 0.5f

/**
 * 무대: 골목 배경 → 고양이(장면이 바뀌면 살짝 떠오르며 교체) → 대사창 뒤를 받쳐 주는 하단 그라데이션 → 사용자 실루엣(전경).
 * 실루엣은 사용자 차례([isUserOnStage])에만 왼쪽에서 들어오고, 고양이·나레이션 차례에는 화면 밖으로 빠진다.
 */
@Composable
internal fun OnboardingStage(
    character: OnboardingCharacter,
    isUserOnStage: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.onboarding_bg_alley_night),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        AnimatedContent(
            targetState = character,
            transitionSpec = {
                (fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 14 }) togetherWith fadeOut(tween(250))
            },
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = maxHeight * CharacterCenterYFraction)
                .fillMaxWidth(CharacterWidthFraction)
                .aspectRatio(1f),
            label = "onboardingCharacter",
        ) { target ->
            target.imageRes?.let { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = stringResource(R.string.onboarding_character_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } ?: Box(Modifier.fillMaxSize())
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(BottomScrimHeightFraction)
                .background(
                    Brush.verticalGradient(
                        listOf(colors.bgScrimGradientTop, colors.bgScrimGradientMiddle, colors.bgScrimGradientBottom),
                    ),
                ),
        )

        AnimatedVisibility(
            visible = isUserOnStage,
            enter = fadeIn(tween(280)) + slideInHorizontally(tween(320)) { -it / 3 },
            exit = fadeOut(tween(220)) + slideOutHorizontally(tween(260)) { -it / 3 },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = maxWidth * SilhouetteXFraction, y = maxHeight * SilhouetteYFraction)
                .fillMaxWidth(SilhouetteWidthFraction)
                .aspectRatio(1f),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_user_silhouette),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
