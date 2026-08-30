package com.dandi.nyummy.auth.presentation

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/**
 * 로그인 랜딩 화면
 *
 * 소셜 로그인(카카오·네이버·구글)은 MVP 에서 UI 만 제공하며 동작하지 않는다.
 * 이메일 원형 버튼만 이메일 로그인 화면으로 이동한다.
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0),
    ) {
        Image(
            painter = painterResource(R.drawable.auth_login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        LoginHeader(
            modifier = Modifier.align(Alignment.TopCenter),
        )
        LoginActions(
            enabled = !uiState.isLoading,
            onIntent = onIntent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LoginHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = LoginHeaderTopPadding)
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DandiText(
            text = stringResource(R.string.auth_login_wordmark),
            modifier = Modifier.fillMaxWidth(),
            color = DesignSystemThemeImpl.designSystemColor.contentBrandWordmark,
            textAlign = TextAlign.Center,
            style = DesignSystemThemeImpl.typeScale.titleStrongL,
        )
        Spacer(modifier = Modifier.height(LoginWordmarkHeadlineSpacing))
        DandiText(
            text = stringResource(R.string.auth_login_headline),
            modifier = Modifier.fillMaxWidth(),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.titleStrongXL,
        )
        Spacer(modifier = Modifier.height(LoginHeadlineSubtitleSpacing))
        DandiText(
            text = stringResource(R.string.auth_login_subtitle),
            modifier = Modifier.fillMaxWidth(),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularL,
        )
    }
}

@Composable
private fun LoginActions(
    enabled: Boolean,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrimBrush = Brush.verticalGradient(
        0f to DesignSystemThemeImpl.designSystemColor.bgScrimGradientTop,
        ScrimGradientMiddleStop to DesignSystemThemeImpl.designSystemColor.bgScrimGradientMiddle,
        1f to DesignSystemThemeImpl.designSystemColor.bgScrimGradientBottom,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(LoginActionsPanelHeight)
            .background(scrimBrush)
            .navigationBarsPadding()
            .padding(horizontal = DesignSystemThemeImpl.designSystemLayout.mobileGutter)
            .padding(bottom = LoginActionsBottomPadding),
        verticalArrangement = Arrangement.spacedBy(
            space = DesignSystemThemeImpl.designSystemSpacing.space24,
            alignment = Alignment.Bottom,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.auth_kakao_login_button),
            contentDescription = stringResource(R.string.auth_login_kakao),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(KakaoButtonAspectRatio)
                .clip(DesignSystemThemeImpl.designSystemShape.buttonDefault)
                .clickable(enabled = enabled, role = Role.Button) {
                    // TODO: 임시로 클릭시 테스트 계정으로 로그인
                    onIntent(LoginIntent.ClickTestLogin)
                    //onIntent(LoginIntent.ClickSocialLogin(SocialLoginType.KAKAO))
                },
            contentScale = ContentScale.Fit,
        )
        LoginOrDivider()
        LoginSocialCircles(
            enabled = enabled,
            onIntent = onIntent,
        )
        DandiText(
            text = stringResource(R.string.auth_login_terms),
            modifier = Modifier.fillMaxWidth(),
            color = DesignSystemThemeImpl.designSystemColor.contentInverseDefault,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
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
                .background(DesignSystemThemeImpl.designSystemColor.contentInverseDefault),
        )
        DandiText(
            text = stringResource(R.string.auth_login_or),
            color = DesignSystemThemeImpl.designSystemColor.contentInverseDefault,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Box(
            modifier = Modifier
                .width(LoginDividerLineWidth)
                .height(LoginDividerLineHeight)
                .background(DesignSystemThemeImpl.designSystemColor.contentInverseDefault),
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

private val LoginHeaderTopPadding = 300.dp
private val LoginWordmarkHeadlineSpacing = 6.dp
private val LoginHeadlineSubtitleSpacing = 10.dp
private val LoginActionsPanelHeight = 424.dp
private val LoginActionsBottomPadding = 40.dp
private val LoginDividerLineWidth = 110.dp
private val LoginDividerLineHeight = 1.dp
private val LoginSocialCircleSize = 56.dp
private val LoginEmailIconSize = 24.dp
private val LoginEmailCircleBorderWidth = 1.dp
private const val KakaoButtonAspectRatio = 600f / 90f
private const val ScrimGradientMiddleStop = 0.35f

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
