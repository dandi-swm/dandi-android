package com.dandi.nyummy.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.dandi.nyummy.auth.presentation.social.SocialLoginResult
import com.dandi.nyummy.auth.presentation.social.launchSocialLogin
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyLoading
import com.dandi.nyummy.common.presentation.component.NyummyLoadingSize
import com.dandi.nyummy.common.presentation.component.NyummyModalScrim
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 로그인 랜딩 화면
 *
 * 카카오 로그인은 SDK 로그인(카카오톡/카카오계정) 후 서버 검증까지 동작한다. 네이버·구글은 아직
 * 연동되지 않아 "준비 중" 안내만 띄운다. 이메일 원형 버튼은 이메일 로그인 화면으로 이동한다.
 * debug 빌드에서 local.properties 에 테스트 계정을 넣으면 하단에 테스트 계정 로그인 버튼이 추가된다.
 */
@Composable
fun LoginPage(
    viewModel: LoginViewModel = hiltViewModel<LoginViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 소셜 SDK 는 Activity 가 필요해 화면이 실행한다. 실행을 먼저 알려 요청 상태를 비워 두므로
    // 로그인 창이 떠 있는 동안 화면이 재생성돼도 다시 실행되지 않는다.
    val activity = LocalActivity.current
    val onIntent = viewModel::onIntent
    LaunchedEffect(uiState.socialLoginToLaunch) {
        val socialType = uiState.socialLoginToLaunch ?: return@LaunchedEffect
        onIntent(LoginIntent.SocialLoginLaunched(socialType))
        val onResult: (SocialLoginResult) -> Unit = { result ->
            onIntent(LoginIntent.SocialLoginResultReceived(socialType, result))
        }
        if (activity == null) {
            onResult(SocialLoginResult.Failed)
        } else {
            launchSocialLogin(socialType, activity, onResult)
        }
    }

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
        SocialLoginVerifyingOverlay(socialType = uiState.verifyingSocialLogin)
    }
}

/**
 * 소셜 로그인 창이 닫힌 뒤 서버 검증이 끝날 때까지 덮는 로딩.
 *
 * 로그인 창에서 돌아오자마자 홈으로 넘어가면 무슨 일이 일어났는지 알기 어려워, 검증 중임을 보여주고
 * 그동안 다른 버튼·뒤로가기 입력을 막는다. 검증이 끝나면 다음 화면으로 넘어가며 함께 사라진다.
 */
@Composable
private fun SocialLoginVerifyingOverlay(socialType: SocialLoginType?) {
    BackHandler(enabled = socialType != null) {}
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = socialType != null,
            enter = fadeIn(tween(VerifyingFadeMillis)),
            exit = fadeOut(tween(VerifyingFadeMillis)),
            label = "SocialLoginVerifyingScrim",
        ) {
            NyummyModalScrim()
        }
        // 사라지는 동안에도 직전 제공자 문구가 유지되도록 상태별 내용을 AnimatedContent 로 전환한다.
        AnimatedContent(
            targetState = socialType,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(VerifyingFadeMillis)) togetherWith fadeOut(tween(VerifyingFadeMillis))
            },
            contentAlignment = Alignment.Center,
            label = "SocialLoginVerifyingCard",
        ) { type ->
            if (type != null) SocialLoginVerifyingCard(socialType = type)
        }
    }
}

@Composable
private fun SocialLoginVerifyingCard(socialType: SocialLoginType) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val spacing = DesignSystemThemeImpl.designSystemSpacing
    val message = stringResource(
        R.string.auth_login_social_verifying,
        stringResource(socialType.providerNameRes),
    )
    Surface(
        shape = RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius24),
        color = colors.bgDefaultLevel1,
        contentColor = colors.contentDefaultLevel0,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.space32, vertical = spacing.space24),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NyummyLoading(size = NyummyLoadingSize.Large, contentDescription = message)
            Spacer(modifier = Modifier.height(spacing.space16))
            DandiText(
                text = message,
                color = colors.contentDefaultLevel0,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
            Spacer(modifier = Modifier.height(spacing.space4))
            DandiText(
                text = stringResource(R.string.auth_login_social_verifying_hint),
                color = colors.contentDefaultLevel1,
                textAlign = TextAlign.Center,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
            )
        }
    }
}

private val SocialLoginType.providerNameRes: Int
    get() = when (this) {
        SocialLoginType.KAKAO -> R.string.auth_social_provider_kakao
        SocialLoginType.GOOGLE -> R.string.auth_social_provider_google
        SocialLoginType.NAVER -> R.string.auth_social_provider_naver
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
private const val VerifyingFadeMillis = 200

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

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPageSocialVerifyingPreview() {
    DesignSystemTheme {
        LoginPageContent(
            uiState = LoginUIState(isLoading = true, verifyingSocialLogin = SocialLoginType.KAKAO),
            onIntent = {},
        )
    }
}
