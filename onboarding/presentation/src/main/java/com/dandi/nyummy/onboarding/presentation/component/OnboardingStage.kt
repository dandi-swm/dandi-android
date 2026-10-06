package com.dandi.nyummy.onboarding.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.onboarding.presentation.OnboardingCharacter
import com.dandi.nyummy.onboarding.presentation.R

/**
 * 온보딩 무대. 밤 골목 배경 → 발밑 그림자 → 냐미 → 대화창을 받쳐 주는 하단 그라데이션 → 사용자 실루엣(전경) 순서로 쌓는다.
 *
 * 해상도 대응:
 * - 배경은 화면을 덮도록(Cover) 키우고 가운데를 기준으로 자른다.
 * - 냐미 크기는 min(폭 × 0.78, 높이 × 0.36, 360).
 * - 냐미 발은 골목 바닥선(배경 높이의 70%)에 둔다. 아래 패널([panelTop])이 바닥선보다 높으면 패널 바로 위로 올리되,
 *   머리가 화면 위쪽 19%보다 올라가지는 않는다(그때는 패널이 발을 가린다). 위치가 바뀔 때는 부드럽게 움직인다.
 * - 사용자 실루엣은 사용자가 말할 때만 왼쪽 아래에서 들어온다.
 */
@Composable
internal fun OnboardingStage(
    character: OnboardingCharacter,
    isUserOnStage: Boolean,
    panelTop: Dp,
    modifier: Modifier = Modifier,
) {
    val background = painterResource(R.drawable.onboarding_bg_alley_night)
    val backgroundRatio = background.intrinsicSize.height / background.intrinsicSize.width
    val shadowColor = NyummyTheme.colors.bg.surfaceInverse
    val gradientColor = NyummyTheme.colors.bg.surfaceInverse

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val backgroundHeight = max(maxHeight, maxWidth * backgroundRatio)
        val backgroundTop = (maxHeight - backgroundHeight) / 2
        val floorY = backgroundTop + backgroundHeight * AlleyFloorRatio

        val heroSize = min(min(maxWidth * HeroWidthRatio, maxHeight * HeroHeightRatio), HeroMaxSize)
        val feetY = min(floorY, panelTop - HeroPanelGap)
        val heroTarget = max(feetY - heroSize * HeroFootRatio, maxHeight * HeroMinTopRatio)
        val heroTop by animateDpAsState(heroTarget, tween(HeroMoveMillis), label = "OnboardingHeroTop")
        val heroLeft = (maxWidth - heroSize) / 2

        Image(
            painter = background,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredSize(backgroundHeight / backgroundRatio, backgroundHeight),
        )

        if (character.imageRes != null) {
            val shadowWidth = heroSize * ShadowWidthRatio
            Canvas(
                modifier = Modifier
                    .offset(x = (maxWidth - shadowWidth) / 2, y = heroTop + heroSize * ShadowCenterRatio - heroSize * ShadowHeightRatio / 2)
                    .size(shadowWidth, heroSize * ShadowHeightRatio),
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(shadowColor.copy(alpha = ShadowAlpha), shadowColor.copy(alpha = 0f)),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width / 2,
                    ),
                )
            }
        }

        AnimatedContent(
            targetState = character,
            transitionSpec = {
                (fadeIn(tween(HeroFadeInMillis)) + slideInVertically(tween(HeroFadeInMillis)) { it / HeroSlideDivisor }) togetherWith
                    fadeOut(tween(HeroFadeOutMillis))
            },
            modifier = Modifier
                .offset(x = heroLeft, y = heroTop)
                .size(heroSize),
            label = "OnboardingCharacter",
        ) { target ->
            val imageRes = target.imageRes
            if (imageRes != null) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = stringResource(R.string.onboarding_character_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(maxHeight / 2)
                .background(
                    Brush.verticalGradient(
                        0f to gradientColor.copy(alpha = 0f),
                        GradientMiddleStop to gradientColor.copy(alpha = GradientMiddleAlpha),
                        1f to gradientColor.copy(alpha = GradientBottomAlpha),
                    ),
                ),
        )

        val silhouetteSize = min(maxWidth * SilhouetteWidthRatio, SilhouetteMaxSize)
        AnimatedVisibility(
            visible = isUserOnStage,
            enter = fadeIn(tween(SilhouetteInMillis)) + slideInHorizontally(tween(SilhouetteInMillis)) { -it / 3 },
            exit = fadeOut(tween(SilhouetteOutMillis)) + slideOutHorizontally(tween(SilhouetteOutMillis)) { -it / 3 },
            modifier = Modifier
                .offset(
                    x = maxWidth * SilhouetteLeftRatio,
                    y = panelTop + silhouetteSize * SilhouetteBelowPanelRatio - silhouetteSize,
                )
                .size(silhouetteSize),
        ) {
            Image(
                painter = painterResource(R.drawable.onboarding_user_silhouette),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 배경 이미지에서 골목 바닥선의 높이 비율. */
private const val AlleyFloorRatio = 0.70f

/** 냐미 이미지에서 발끝이 있는 높이 비율. */
private const val HeroFootRatio = 0.968f
private const val HeroWidthRatio = 0.78f
private const val HeroHeightRatio = 0.36f
private val HeroMaxSize = 360.dp
private val HeroPanelGap = 4.dp

/** 냐미 머리가 이보다 위로 올라가지 않는다(화면 높이 비율). */
private const val HeroMinTopRatio = 0.19f
private const val HeroMoveMillis = 300
private const val HeroFadeInMillis = 450
private const val HeroFadeOutMillis = 250
private const val HeroSlideDivisor = 14

/** 발밑 그림자: 냐미 크기 대비 폭, 높이, 중심 높이. */
private const val ShadowWidthRatio = 0.62f
private const val ShadowHeightRatio = 0.08f
private const val ShadowCenterRatio = 0.94f
private const val ShadowAlpha = 0.6f

private const val GradientMiddleStop = 0.55f
private const val GradientMiddleAlpha = 0.45f
private const val GradientBottomAlpha = 0.75f

/** 사용자 실루엣: 화면 폭 대비 크기와 왼쪽 위치, 패널 위쪽 아래로 내려가는 비율. */
private const val SilhouetteWidthRatio = 0.62f
private val SilhouetteMaxSize = 300.dp
private const val SilhouetteLeftRatio = -0.12f
private const val SilhouetteBelowPanelRatio = 0.186f
private const val SilhouetteInMillis = 300
private const val SilhouetteOutMillis = 240
