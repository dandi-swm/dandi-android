package com.dandi.nyummy.intro.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 인트로 스플래시 콘텐츠 — 배경 일러스트(냐미) 위에 워드마크·헤드라인·말풍선·진행 카드·팁을 얹는다.
 *
 * 진행바는 실제 진행률이 없는 장식이다. 권한 안내([isPermissionNoticeVisible])가 떠 있는 동안은
 * 0 에 멈춰 있다가("권한부터 받고 로딩"), 닫히면 90% 까지 차오르고,
 * 시작 게이트가 끝나면([isComplete]) 100% 까지 빠르게 채운다.
 * 퍼센트 텍스트는 같은 애니메이션 값에서 파생돼 진행바와 동기화된다.
 */
@Composable
fun IntroSplashContent(
    isComplete: Boolean,
    isPermissionNoticeVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing

    var decorativeTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { decorativeTarget = SplashProgressTarget }
    val progress by animateFloatAsState(
        targetValue = when {
            isComplete -> 1f
            isPermissionNoticeVisible -> 0f
            else -> decorativeTarget
        },
        animationSpec = tween(
            durationMillis = if (isComplete) SplashCompleteDurationMillis else SplashProgressDurationMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "IntroSplashProgress",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgDefaultLevel0),
    ) {
        Image(
            painter = painterResource(R.drawable.intro_splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(SplashTopSpacing))
            SplashWordmark()
            Spacer(Modifier.height(spacing.space12))
            DandiText(
                text = stringResource(R.string.intro_splash_headline_line1),
                color = colors.contentDefaultLevel1,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.displayRegularXXL,
            )
            DandiText(
                text = stringResource(R.string.intro_splash_headline_line2),
                color = colors.contentBrandWordmark,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.displayRegularXXL,
            )
            Spacer(Modifier.height(spacing.space16))
            SplashSpeechBubble()
            Spacer(Modifier.weight(1f))
            SplashProgressCard(progress = progress)
            Spacer(Modifier.height(spacing.space16))
            SplashTipRow()
            Spacer(Modifier.height(spacing.space24))
        }
    }
}

@Composable
private fun SplashWordmark() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.intro_icon_paw),
            contentDescription = null,
            modifier = Modifier.size(SplashPawIconSize),
            tint = colors.contentBrandWordmark,
        )
        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
        DandiText(
            text = stringResource(R.string.intro_splash_wordmark),
            color = colors.contentBrandWordmark,
            style = DesignSystemThemeImpl.typeScale.displayRegularM,
        )
        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
        Icon(
            painter = painterResource(R.drawable.intro_icon_paw),
            contentDescription = null,
            modifier = Modifier.size(SplashPawIconSize),
            tint = colors.contentBrandWordmark,
        )
    }
}

@Composable
private fun SplashSpeechBubble() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        shape = DesignSystemThemeImpl.designSystemShape.pill,
        color = colors.bgDefaultLevel1,
        border = BorderStroke(SplashBorderWidth, colors.borderDefaultLevel1),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = DesignSystemThemeImpl.designSystemSpacing.space16,
                vertical = DesignSystemThemeImpl.designSystemSpacing.space8,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.intro_icon_heart),
                contentDescription = null,
                modifier = Modifier.size(SplashHeartIconSize),
                tint = colors.contentAccentCoral,
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            DandiText(
                text = stringResource(R.string.intro_splash_bubble),
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
            )
            Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            Icon(
                painter = painterResource(R.drawable.intro_icon_heart),
                contentDescription = null,
                modifier = Modifier.size(SplashHeartIconSize),
                tint = colors.contentAccentCoral,
            )
        }
    }
}

@Composable
private fun SplashProgressCard(
    progress: Float,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius20),
        color = colors.bgDefaultLevel1,
        border = BorderStroke(SplashBorderWidth, colors.borderDefaultLevel1),
    ) {
        Column(
            modifier = Modifier.padding(DesignSystemThemeImpl.designSystemSpacing.space20),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DandiText(
                text = stringResource(R.string.intro_splash_progress_label),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongM,
            )
            Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space12))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NyummyLinearProgress(
                    progress = progress,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space12))
                DandiText(
                    text = stringResource(
                        R.string.intro_splash_progress_percent,
                        (progress * 100).toInt(),
                    ),
                    color = colors.contentActionSecondary,
                    style = DesignSystemThemeImpl.typeScale.numberStrongM,
                )
            }
        }
    }
}

@Composable
private fun SplashTipRow() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.intro_icon_bulb),
            contentDescription = null,
            modifier = Modifier.size(SplashBulbIconSize),
            tint = colors.contentIconWarning,
        )
        Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space4))
        DandiText(
            text = stringResource(R.string.intro_splash_tip),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
    }
}

/** 진행바 장식 애니메이션의 목표치 — 게이트 완료 전에는 90% 에서 멈춘다. */
private const val SplashProgressTarget = 0.9f
private const val SplashProgressDurationMillis = 1500

/** 게이트 완료 시 100% 채움 시간. IntroViewModel 의 이동 대기(500ms)보다 짧아야 한다. */
private const val SplashCompleteDurationMillis = 250
private val SplashTopSpacing = 96.dp
private val SplashPawIconSize = 18.dp
private val SplashHeartIconSize = 14.dp
private val SplashBulbIconSize = 16.dp
private val SplashBorderWidth = 1.dp

@Preview(name = "Intro Splash", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun IntroSplashContentPreview() {
    DesignSystemTheme {
        IntroSplashContent(isComplete = false, isPermissionNoticeVisible = false)
    }
}
