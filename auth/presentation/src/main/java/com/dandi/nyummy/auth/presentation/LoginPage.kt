package com.dandi.nyummy.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.presentation.social.SocialLoginResult
import com.dandi.nyummy.auth.presentation.social.launchSocialLogin
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.foundation.nyummyClickable
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.designsystem.theme.nyummyShadow
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 로그인 랜딩 화면.
 *
 * 민트 낙서 패턴 배경 위에 로고와 헤드라인을 두고, 아래 카드에 카카오 로그인과 다른 방법(네이버, 구글, 이메일)을 둔다.
 * 카카오와 구글은 SDK 로그인 후 서버 검증까지 동작하고, 네이버는 "준비 중" 안내만 띄운다.
 * debug 빌드에서 local.properties에 테스트 계정을 넣으면 카드 아래쪽에 테스트 계정 로그인이 추가된다.
 */
@Composable
fun LoginPage(
    viewModel: LoginViewModel = hiltViewModel<LoginViewModel>(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 소셜 SDK는 Activity가 필요해 화면이 실행한다. 실행을 먼저 알려 요청 상태를 비워 두므로
    // 로그인 창이 떠 있는 동안 화면이 재생성돼도 다시 실행되지 않는다.
    val activity = LocalActivity.current
    val onIntent = viewModel::onIntent
    LaunchedEffect(uiState.socialLoginToLaunch) {
        val attempt = uiState.socialLoginToLaunch ?: return@LaunchedEffect
        onIntent(LoginIntent.SocialLoginLaunched(attempt))
        val onResult: (SocialLoginResult) -> Unit = { result ->
            onIntent(LoginIntent.SocialLoginResultReceived(attempt, result))
        }
        if (activity == null) {
            onResult(SocialLoginResult.Failed)
        } else {
            launchSocialLogin(attempt.socialType, activity, onResult)
        }
    }

    LoginPageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun LoginPageContent(
    uiState: LoginUIState,
    onIntent: (LoginIntent) -> Unit,
) {
    val theme = NyummyTheme
    val enabled = !uiState.isLoading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.colors.bg.canvas),
    ) {
        // 반복 패턴이라 어떤 화면 비율로 잘려도 어색하지 않다. 50%로 옅게 깐다.
        Image(
            painter = painterResource(R.drawable.auth_bg_pattern),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(PatternAlpha),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = theme.spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 제목 블록은 상단과 카드 사이 빈 공간의 가운데보다 살짝 위에 둔다.
            Spacer(Modifier.weight(TitleTopWeight))
            LoginTitleBlock()
            Spacer(Modifier.weight(TitleBottomWeight))
            LoginCard(
                uiState = uiState,
                enabled = enabled,
                onIntent = onIntent,
                modifier = Modifier.widthIn(max = CardMaxWidth),
            )
            Spacer(Modifier.height(theme.spacing.s24))
        }
        SocialLoginLoadingOverlay(
            socialType = uiState.socialLoginInProgress,
            isVerifying = uiState.verifyingSocialLogin != null,
            onBackPressed = { onIntent(LoginIntent.SocialLoginBackPressed) },
        )
    }
}

@Composable
private fun LoginTitleBlock() {
    val theme = NyummyTheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TitleBlockGap),
    ) {
        Image(
            painter = painterResource(CommonR.drawable.nyummy_brand_logo),
            contentDescription = stringResource(R.string.auth_login_wordmark),
            modifier = Modifier.size(LogoWidth, LogoHeight),
        )
        NyummyText(
            text = stringResource(R.string.auth_login_headline),
            style = theme.typography.displayL,
            color = theme.colors.content.brand,
            textAlign = TextAlign.Center,
        )
        NyummyText(
            text = stringResource(R.string.auth_login_subtitle),
            style = theme.typography.bodyM,
            color = theme.colors.content.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** 카카오 로그인 + "다른 방법으로 시작하기" + 원형 버튼 3개 + 약관 고지를 담은 하단 카드. */
@Composable
private fun LoginCard(
    uiState: LoginUIState,
    enabled: Boolean,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = NyummyTheme
    val shape = RoundedCornerShape(theme.radius.l)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .nyummyShadow(shape, theme.elevation.float)
            .background(theme.colors.bg.surface, shape)
            .padding(theme.spacing.s20),
        verticalArrangement = Arrangement.spacedBy(theme.spacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KakaoLoginButton(
            enabled = enabled,
            onClick = { onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO)) },
        )
        LoginOrDivider()
        LoginSocialCircles(enabled = enabled, onIntent = onIntent)
        LoginTermsNotice()
        if (uiState.isTestLoginAvailable) {
            NyummyTextButton(
                text = stringResource(R.string.auth_login_test_account),
                onClick = { onIntent(LoginIntent.ClickTestLogin) },
                tone = NyummyTextButtonTone.Neutral,
                size = NyummyTextButtonSize.S,
                enabled = enabled,
            )
        }
    }
}

/**
 * 카카오 공식 로그인 버튼. 카카오 노랑, radius 12, 높이 52.
 * 심볼은 왼쪽에 고정하고 라벨은 가운데에 둔다. 재채색이나 변형은 하지 않는다.
 */
@Composable
private fun KakaoLoginButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val theme = NyummyTheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .nyummyClickable(onClick = onClick, enabled = enabled)
            .height(KakaoButtonHeight)
            .clip(RoundedCornerShape(theme.radius.s))
            .background(theme.colors.external.kakaoYellow),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.auth_ic_kakao_symbol),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = KakaoSymbolStartPadding)
                .size(KakaoSymbolSize),
        )
        NyummyText(
            text = stringResource(R.string.auth_login_kakao),
            style = theme.typography.labelM,
            color = theme.colors.external.kakaoLabel,
        )
    }
}

@Composable
private fun LoginOrDivider() {
    val theme = NyummyTheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DividerLine()
        NyummyText(
            text = stringResource(R.string.auth_login_or),
            style = theme.typography.bodyS,
            color = theme.colors.content.tertiary,
        )
        DividerLine()
    }
}

@Composable
private fun DividerLine() {
    Box(
        Modifier
            .size(DividerLineWidth, NyummyTheme.borderWidth.hairline)
            .background(NyummyTheme.colors.border.default),
    )
}

/** 네이버와 구글은 공식 원형 에셋(재채색 금지), 이메일은 냐미 스타일 원형 버튼. */
@Composable
private fun LoginSocialCircles(
    enabled: Boolean,
    onIntent: (LoginIntent) -> Unit,
) {
    val theme = NyummyTheme
    Row(horizontalArrangement = Arrangement.spacedBy(theme.spacing.s24)) {
        SocialCircle(
            description = stringResource(R.string.auth_login_naver_description),
            enabled = enabled,
            onClick = { onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.NAVER)) },
        ) {
            Image(painterResource(R.drawable.auth_icon_naver_circle), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        SocialCircle(
            description = stringResource(R.string.auth_login_google_description),
            enabled = enabled,
            onClick = { onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.GOOGLE)) },
        ) {
            Image(painterResource(R.drawable.auth_google_button), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
        SocialCircle(
            description = stringResource(R.string.auth_login_email_description),
            enabled = enabled,
            onClick = { onIntent(LoginIntent.ClickEmailLogin) },
            modifier = Modifier
                .background(theme.colors.bg.surface, CircleShape)
                .border(theme.borderWidth.bold, theme.colors.border.default, CircleShape),
        ) {
            Icon(
                painter = painterResource(CommonR.drawable.nyummy_ic_mail),
                contentDescription = null,
                tint = theme.colors.content.brand,
                modifier = Modifier.size(theme.size.iconL),
            )
        }
    }
}

@Composable
private fun SocialCircle(
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .semantics { contentDescription = description }
            .nyummyClickable(onClick = onClick, enabled = enabled)
            .size(SocialCircleSize)
            .clip(CircleShape)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * "계속하면 [이용약관] 및 [개인정보처리방침]에 동의하게 됩니다." 고지.
 * 한 문장으로 조합해 자동 줄바꿈되므로 긴 로케일에서도 잘리지 않고, 링크 부분만 밑줄과 진한 글자로 둔다.
 */
@Composable
private fun LoginTermsNotice() {
    val theme = NyummyTheme
    val link = SpanStyle(textDecoration = TextDecoration.Underline, color = theme.colors.content.secondary)
    val notice = buildAnnotatedString {
        append(stringResource(R.string.auth_login_terms_prefix))
        withStyle(link) { append(stringResource(R.string.auth_login_terms_service)) }
        append(stringResource(R.string.auth_login_terms_and))
        withStyle(link) { append(stringResource(R.string.auth_login_terms_privacy)) }
        append(stringResource(R.string.auth_login_terms_suffix))
    }
    NyummyText(
        text = notice,
        style = theme.typography.bodyS,
        color = theme.colors.content.tertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 소셜 로그인 창에서 돌아온 뒤 홈이나 가입 화면으로 넘어갈 때까지 덮는 로딩.
 *
 * 창이 닫힌 뒤에도 카카오 토큰 발급(약 1초)과 서버 검증이 이어지는데, 그동안 로그인 화면만 보이다가
 * 갑자기 홈으로 넘어가면 무슨 일이 일어났는지 알기 어렵다. 그 구간을 로딩으로 덮고 다른 입력을 막는다.
 *
 * 카카오 창이 떠 있는 동안에는 이 화면이 가려져 있으므로, 화면이 다시 보인 뒤에만 띄운다. 그리고
 * [LoadingShowDelayMillis]만큼 기다렸다 나타나게 해, 창을 닫아 취소한 경우 로딩이 번쩍이지 않게 한다.
 * 결과 대기 중 뒤로 가기는 기다림을 그만두고, 서버 검증 중에는 막힌다(ViewModel이 판단).
 */
@Composable
private fun SocialLoginLoadingOverlay(
    socialType: SocialLoginType?,
    isVerifying: Boolean,
    onBackPressed: () -> Unit,
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val isScreenResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val shownType = socialType?.takeIf { isScreenResumed || isVerifying }

    BackHandler(enabled = socialType != null, onBack = onBackPressed)
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = shownType != null,
            enter = fadeIn(tween(LoadingFadeMillis, delayMillis = LoadingShowDelayMillis)),
            exit = fadeOut(tween(LoadingFadeMillis)),
            label = "SocialLoginLoadingScrim",
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(NyummyTheme.colors.bg.scrim)
                    .pointerInput(Unit) { detectTapGestures { } },
            )
        }
        // 사라지는 동안에도 직전 제공자 문구가 유지되도록 상태별 내용을 AnimatedContent로 전환한다.
        AnimatedContent(
            targetState = shownType,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = NyummyTheme.spacing.gutter),
            transitionSpec = {
                fadeIn(tween(LoadingFadeMillis, delayMillis = LoadingShowDelayMillis)) togetherWith
                    fadeOut(tween(LoadingFadeMillis))
            },
            label = "SocialLoginLoadingCard",
        ) { type ->
            if (type != null) SocialLoginLoadingCard(socialType = type)
        }
    }
}

@Composable
private fun SocialLoginLoadingCard(socialType: SocialLoginType) {
    val theme = NyummyTheme
    val shape = RoundedCornerShape(theme.radius.l)
    val message = stringResource(
        R.string.auth_login_social_verifying,
        stringResource(socialType.providerNameRes),
    )
    val rotation by rememberInfiniteTransition(label = "SocialLoginSpinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SpinnerRotationMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "SocialLoginSpinnerRotation",
    )
    Column(
        modifier = Modifier
            .nyummyShadow(shape, theme.elevation.float)
            .background(theme.colors.bg.surface, shape)
            .padding(horizontal = theme.spacing.s32, vertical = theme.spacing.s24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(CommonR.drawable.nyummy_ic_loader_circle),
            contentDescription = message,
            tint = theme.colors.content.brand,
            modifier = Modifier
                .size(SpinnerSize)
                .graphicsLayer { rotationZ = rotation },
        )
        Spacer(Modifier.height(SpinnerTextGap))
        NyummyText(text = message, style = theme.typography.titleS, textAlign = TextAlign.Center)
        Spacer(Modifier.height(LoadingTextGap))
        NyummyText(
            text = stringResource(R.string.auth_login_social_verifying_hint),
            style = theme.typography.bodyM,
            color = theme.colors.content.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

private val SocialLoginType.providerNameRes: Int
    get() = when (this) {
        SocialLoginType.KAKAO -> R.string.auth_social_provider_kakao
        SocialLoginType.GOOGLE -> R.string.auth_social_provider_google
        SocialLoginType.NAVER -> R.string.auth_social_provider_naver
    }

private const val PatternAlpha = 0.5f
private const val TitleTopWeight = 1f
private const val TitleBottomWeight = 1.3f
private const val LoadingFadeMillis = 200
private const val LoadingShowDelayMillis = 150
private const val SpinnerRotationMillis = 900
private val CardMaxWidth = 480.dp
private val LogoWidth = 150.dp
private val LogoHeight = 88.dp
private val TitleBlockGap = 14.dp
private val KakaoButtonHeight = 52.dp
private val KakaoSymbolStartPadding = 18.dp
private val KakaoSymbolSize = 20.dp
private val DividerLineWidth = 56.dp
private val SocialCircleSize = 56.dp
private val SpinnerSize = 36.dp
private val SpinnerTextGap = 16.dp
private val LoadingTextGap = 6.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPagePreview() {
    NyummyTheme {
        LoginPageContent(uiState = LoginUIState.empty, onIntent = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginPageSocialVerifyingPreview() {
    NyummyTheme {
        LoginPageContent(
            uiState = LoginUIState(
                isLoading = true,
                verifyingSocialLogin = SocialLoginAttempt(id = 1, socialType = SocialLoginType.KAKAO),
            ),
            onIntent = {},
        )
    }
}
