package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 로그인 랜딩 화면. Figma `MOB/LIVE/AUTH-01/Default`(73:53).
 *
 * 소셜 로그인(카카오·네이버·구글)은 MVP 에서 UI 만 제공하며 동작하지 않는다.
 * 이메일 원형 버튼만 이메일 로그인 화면으로 이동한다.
 */
@Composable
fun LoginPage(
    onEmailLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgDefaultLevel0),
    ) {
        Image(
            painter = painterResource(R.drawable.auth_login_bg),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )

        LoginTitleBlock(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )

        LoginActions(
            onEmailLoginClick = onEmailLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
    }
}

/** 일러스트 위 워드마크·헤드라인 묶음 (Figma y≈300/844 위치를 비율로 재현). */
@Composable
private fun LoginTitleBlock(modifier: Modifier = Modifier) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(TitleTopWeight))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DandiText(
                text = stringResource(R.string.auth_login_wordmark),
                color = colors.brandWordmark,
                style = DesignSystemThemeImpl.typeScale.titleStrongL,
            )
            Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space4))
            DandiText(
                text = stringResource(R.string.auth_login_headline),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.titleStrongL,
            )
            Spacer(modifier = Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space12))
            DandiText(
                text = stringResource(R.string.auth_login_subtitle),
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textRegularL,
            )
        }
        Spacer(modifier = Modifier.weight(1f - TitleTopWeight))
    }
}

/** 하단 그라데이션 스크림 위 로그인 액션 묶음. */
@Composable
private fun LoginActions(
    onEmailLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val scrimBase = colors.bgSurfaceInverse

    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    0f to scrimBase.copy(alpha = 0f),
                    ScrimMidStop to scrimBase.copy(alpha = ScrimMidAlpha),
                    1f to scrimBase.copy(alpha = ScrimEndAlpha),
                ),
            )
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter)
            .navigationBarsPadding()
            .padding(top = ActionsTopPadding, bottom = ActionsBottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space24),
    ) {
        KakaoLoginButton(onClick = {})
        OrDivider()
        SocialCircleRow(onEmailLoginClick = onEmailLoginClick)
        DandiText(
            text = stringResource(R.string.auth_login_terms),
            color = colors.contentInverseDefault,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 카카오 로그인 버튼 (UI 전용 — MVP 미동작).
 *
 * 카카오 개발자센터 리소스 다운로드의 공식 버튼 에셋(large_wide, ko/en 로케일별)을
 * 그대로 그린다 — 심볼·라벨·색·radius 전부 에셋에 포함.
 */
@Composable
private fun KakaoLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius12),
        color = Color.Transparent,
    ) {
        Image(
            painter = painterResource(R.drawable.auth_kakao_login_button),
            contentDescription = stringResource(R.string.auth_login_kakao),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(KakaoButtonAspectRatio),
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
private fun OrDivider(modifier: Modifier = Modifier) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12),
    ) {
        Box(
            modifier = Modifier
                .width(DividerLineWidth)
                .height(DividerLineHeight)
                .background(colors.contentInverseDefault),
        )
        DandiText(
            text = stringResource(R.string.auth_login_or),
            color = colors.contentInverseDefault,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Box(
            modifier = Modifier
                .width(DividerLineWidth)
                .height(DividerLineHeight)
                .background(colors.contentInverseDefault),
        )
    }
}

/** 네이버·구글(UI 전용)·이메일 원형 버튼 줄. */
@Composable
private fun SocialCircleRow(
    onEmailLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space24),
    ) {
        Surface(
            onClick = {},
            modifier = Modifier.size(SocialCircleSize),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Image(
                painter = painterResource(R.drawable.auth_icon_naver_circle),
                contentDescription = stringResource(R.string.auth_login_naver_description),
                modifier = Modifier.size(SocialCircleSize),
            )
        }
        // 구글은 로고 재제작이 금지라 공식 아이콘 전용 버튼 에셋(signin-assets)을 그대로 그린다.
        Surface(
            onClick = {},
            modifier = Modifier.size(SocialCircleSize),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Image(
                painter = painterResource(R.drawable.auth_google_icon_button),
                contentDescription = stringResource(R.string.auth_login_google_description),
                modifier = Modifier.size(SocialCircleSize),
            )
        }
        SocialCircleButton(
            iconRes = R.drawable.auth_icon_email,
            contentDescription = stringResource(R.string.auth_login_email_description),
            borderColor = colors.borderDefaultLevel0,
            onClick = onEmailLoginClick,
        )
    }
}

@Composable
private fun SocialCircleButton(
    iconRes: Int,
    contentDescription: String,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor

    Surface(
        onClick = onClick,
        modifier = modifier.size(SocialCircleSize),
        shape = CircleShape,
        color = colors.bgDefaultLevel1,
        border = androidx.compose.foundation.BorderStroke(SocialCircleBorderWidth, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(SocialCircleIconSize),
            )
        }
    }
}

private const val TitleTopWeight = 0.36f
private const val ScrimMidStop = 0.35f
private const val ScrimMidAlpha = 0.45f
private const val ScrimEndAlpha = 0.85f
private val ActionsTopPadding = 120.dp
private val ActionsBottomPadding = 40.dp
/** 공식 에셋 kakao_login_large_wide.png 원본 비율 (600×90). */
private const val KakaoButtonAspectRatio = 600f / 90f
private val DividerLineWidth = 110.dp
private val DividerLineHeight = 1.dp
private val SocialCircleSize = 56.dp
private val SocialCircleIconSize = 24.dp
private val SocialCircleBorderWidth = 1.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPagePreview() {
    DesignSystemTheme {
        LoginPage(onEmailLoginClick = {})
    }
}
