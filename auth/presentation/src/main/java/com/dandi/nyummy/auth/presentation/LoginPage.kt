package com.dandi.nyummy.auth.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 로그인 랜딩 화면
 *
 * 소셜 로그인(카카오·네이버·구글)은 MVP 에서 UI 만 제공하며 동작하지 않는다.
 * 이메일 원형 버튼만 이메일 로그인 화면으로 이동한다.
 * debug 빌드에서 local.properties 에 테스트 계정을 넣으면 하단에 테스트 계정 로그인 버튼이 추가된다.
 */
@Composable
fun LoginPage(
    viewModel: LoginViewModel = hiltViewModel<LoginViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginPageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun LoginPageContent(
    uiState: LoginUIState,
    onIntent: (LoginIntent) -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    val enabled = !uiState.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgDefaultLevel0),
    ) {
        // 은은한 패턴 배경이라 별도 스크림 없이도 전 영역에서 텍스트 가독성이 유지된다.
        Image(
            painter = painterResource(CommonR.drawable.nyummy_pattern_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
        ) {
            Spacer(modifier = Modifier.weight(LogoTopWeight))
            LoginWordmark(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.weight(LogoHeadlineWeight))
            DandiText(
                text = stringResource(R.string.auth_login_headline_line1),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularXXL,
            )
            DandiText(
                text = stringResource(R.string.auth_login_headline_line2),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularXXL,
            )
            Spacer(modifier = Modifier.height(spacing.space12))
            DandiText(
                text = stringResource(R.string.auth_login_subtitle),
                color = colors.contentDefaultLevel1,
                maxLines = 2,
                style = DesignSystemThemeImpl.typeScale.textRegularL,
            )
            Spacer(modifier = Modifier.height(spacing.space24))
            KakaoLoginButton(
                enabled = enabled,
                onClick = { onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO)) },
            )
            Spacer(modifier = Modifier.height(spacing.space16))
            LoginOrDivider(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(spacing.space16))
            LoginSocialCircles(
                enabled = enabled,
                onIntent = onIntent,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(spacing.space16))
            LoginTermsNotice(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(spacing.space16))
            if (uiState.isTestLoginAvailable) {
                LoginTestAccountButton(
                    enabled = enabled,
                    onClick = { onIntent(LoginIntent.ClickTestLogin) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(spacing.space16))
            }
        }
    }
}

/** 새싹이 돋은 `냐미` 로고 이미지. */
@Composable
private fun LoginWordmark(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.auth_logo),
        contentDescription = stringResource(R.string.auth_login_wordmark),
        modifier = modifier.width(LogoWidth),
        contentScale = ContentScale.Fit,
    )
}

/** 카카오 심볼 + 라벨을 중앙 정렬한 카카오 브랜드 버튼. */
@Composable
private fun KakaoLoginButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(KakaoButtonHeight)
            .clip(DesignSystemThemeImpl.designSystemShape.buttonDefault)
            .background(colors.bgBrandKakao)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.auth_icon_kakao_bubble),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = KakaoBubbleStartPadding)
                .size(KakaoBubbleIconSize),
            tint = colors.contentBrandKakao,
        )
        DandiText(
            text = stringResource(R.string.auth_login_kakao),
            color = colors.contentBrandKakao,
            style = DesignSystemThemeImpl.typeScale.textStrongL,
        )
    }
}

@Composable
private fun LoginOrDivider(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(LoginDividerLineWidth)
                .height(LoginDividerLineHeight)
                .background(DesignSystemThemeImpl.designSystemColor.borderDefaultLevel0),
        )
        DandiText(
            text = stringResource(R.string.auth_login_or),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Box(
            modifier = Modifier
                .width(LoginDividerLineWidth)
                .height(LoginDividerLineHeight)
                .background(DesignSystemThemeImpl.designSystemColor.borderDefaultLevel0),
        )
    }
}

@Composable
private fun LoginSocialCircles(
    enabled: Boolean,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space24),
    ) {
        // 네이버 브랜드 가이드 원형 버튼 — 틴트 금지
        Image(
            painter = painterResource(R.drawable.auth_icon_naver_circle),
            contentDescription = stringResource(R.string.auth_login_naver_description),
            modifier = Modifier
                .size(LoginSocialCircleSize)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button) {
                    onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.NAVER))
                },
        )
        Image(
            painter = painterResource(R.drawable.auth_google_icon_button),
            contentDescription = stringResource(R.string.auth_login_google_description),
            modifier = Modifier
                .size(LoginSocialCircleSize)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button) {
                    onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.GOOGLE))
                },
        )
        Box(
            modifier = Modifier
                .size(LoginSocialCircleSize)
                .clip(CircleShape)
                .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1)
                .border(
                    width = LoginEmailCircleBorderWidth,
                    color = DesignSystemThemeImpl.designSystemColor.borderDefaultLevel0,
                    shape = CircleShape,
                )
                .clickable(enabled = enabled, role = Role.Button) {
                    onIntent(LoginIntent.ClickEmailLogin)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.auth_icon_email),
                contentDescription = stringResource(R.string.auth_login_email_description),
                modifier = Modifier.size(LoginEmailIconSize),
                tint = DesignSystemThemeImpl.designSystemColor.contentIconEmail,
            )
        }
    }
}

/**
 * "계속하면 [이용약관] 및 [개인정보처리방침]에 동의하게 됩니다." 고지.
 * 한 문장으로 조합해 자동 줄바꿈되므로 긴 로케일에서도 잘리지 않고, 링크 부분만 밑줄 처리한다.
 */
@Composable
private fun LoginTermsNotice(
    modifier: Modifier = Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val underline = SpanStyle(textDecoration = TextDecoration.Underline)
    val notice = buildAnnotatedString {
        append(stringResource(R.string.auth_login_terms_prefix))
        withStyle(underline) { append(stringResource(R.string.auth_login_terms_service)) }
        append(stringResource(R.string.auth_login_terms_and))
        withStyle(underline) { append(stringResource(R.string.auth_login_terms_privacy)) }
        append(stringResource(R.string.auth_login_terms_suffix))
        append(" ")
        append(stringResource(R.string.auth_login_terms_line2))
    }
    DandiText(
        text = notice,
        modifier = modifier,
        color = colors.contentDefaultLevel2,
        textAlign = TextAlign.Center,
        maxLines = 3,
        style = DesignSystemThemeImpl.typeScale.textRegularS,
    )
}

/** 개발용 테스트 계정 로그인. debug 빌드에 테스트 계정이 주입됐을 때만 노출된다. */
@Composable
private fun LoginTestAccountButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    DandiText(
        text = stringResource(R.string.auth_login_test_account),
        modifier = modifier
            .clip(DesignSystemThemeImpl.designSystemShape.buttonDefault)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.space12, vertical = spacing.space8),
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
        textDecoration = TextDecoration.Underline,
        style = DesignSystemThemeImpl.typeScale.textRegularS,
    )
}

private const val LogoTopWeight = 0.9f
private const val LogoHeadlineWeight = 1f
private val LogoWidth = 150.dp
private val KakaoButtonHeight = 56.dp
private val KakaoBubbleStartPadding = 20.dp
private val KakaoBubbleIconSize = 20.dp
private val LoginDividerLineWidth = 72.dp
private val LoginDividerLineHeight = 1.dp
private val LoginSocialCircleSize = 56.dp
private val LoginEmailIconSize = 24.dp
private val LoginEmailCircleBorderWidth = 1.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPagePreview() {
    DesignSystemTheme {
        LoginPageContent(
            uiState = LoginUIState.empty,
            onIntent = {},
        )
    }
}
