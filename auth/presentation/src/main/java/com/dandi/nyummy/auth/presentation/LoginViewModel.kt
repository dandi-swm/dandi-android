package com.dandi.nyummy.auth.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.EmailLoginPage
import com.dandi.nyummy.auth.domain.LoginUseCase
import com.dandi.nyummy.auth.domain.SocialLoginUseCase
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.auth.presentation.social.SocialLoginResult
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val navigationHelper: NavigationHelper,
    val loginUseCase: LoginUseCase,
    private val socialLoginUseCase: SocialLoginUseCase,
) :
    MviViewModel<LoginIntent, LoginUIState, LoginReducerEvent>(
        LoginUIState(isTestLoginAvailable = isTestLoginAvailable)
    ) {
    override fun onIntent(intent: LoginIntent) {

        when (intent) {
            is LoginIntent.ClickSocialLogin -> requestSocialLogin(intent.socialType)
            is LoginIntent.SocialLoginLaunched -> onSocialLoginLaunched(intent.socialType)
            is LoginIntent.SocialLoginResultReceived ->
                onSocialLoginResult(intent.socialType, intent.result)
            LoginIntent.SocialLoginBackPressed -> abandonSocialLogin()
            LoginIntent.ClickEmailLogin -> emailLogin()
            LoginIntent.ClickTestLogin -> testLogin()
        }
    }

    override fun reduce(
        state: LoginUIState,
        event: LoginReducerEvent
    ): LoginUIState {
        return when (event) {
            is LoginReducerEvent.SocialLoginStarted ->
                state.copy(isLoading = true, socialLoginToLaunch = event.socialType)
            is LoginReducerEvent.SocialLoginLaunched ->
                state.copy(socialLoginToLaunch = null, awaitingSocialLogin = event.socialType)
            is LoginReducerEvent.SocialLoginVerifying ->
                state.copy(awaitingSocialLogin = null, verifyingSocialLogin = event.socialType)
            is LoginReducerEvent.LoginFinished -> state.copy(
                isLoading = false,
                socialLoginToLaunch = null,
                awaitingSocialLogin = null,
                verifyingSocialLogin = null,
            )
            is LoginReducerEvent.EmailLoginClicked -> state.copy(isLoading = false)
            is LoginReducerEvent.TestLoginClicked -> state.copy(isLoading = true)
        }
    }

    private fun emailLogin() {
        dispatch(LoginReducerEvent.EmailLoginClicked)
        navigationHelper.navigateTo(EmailLoginPage)
    }

    /** 화면에 SDK 실행을 요청한다. 다른 로그인이 진행 중이면(연타 포함) 무시한다. */
    private fun requestSocialLogin(socialType: SocialLoginType) {
        if (currentState.isLoading) return
        dispatch(LoginReducerEvent.SocialLoginStarted(socialType))
    }

    private fun onSocialLoginLaunched(socialType: SocialLoginType) {
        if (currentState.socialLoginToLaunch != socialType) return
        dispatch(LoginReducerEvent.SocialLoginLaunched(socialType))
    }

    /**
     * SDK 결과를 처리한다. 기다리던 시도의 결과만 받는다.
     * 자격 증명을 얻으면 서버 검증으로 넘기고, 성공 시 이동·실패 안내는 UseCase 가 처리한다.
     */
    private fun onSocialLoginResult(socialType: SocialLoginType, result: SocialLoginResult) {
        if (currentState.awaitingSocialLogin != socialType) return
        when (result) {
            is SocialLoginResult.Success -> {
                dispatch(LoginReducerEvent.SocialLoginVerifying(socialType))
                viewModelScope.launch {
                    socialLoginUseCase.login(result.credential)
                    dispatch(LoginReducerEvent.LoginFinished)
                }
            }

            SocialLoginResult.Cancelled -> dispatch(LoginReducerEvent.LoginFinished)

            SocialLoginResult.Failed -> {
                socialLoginUseCase.onProviderFailed(socialType)
                dispatch(LoginReducerEvent.LoginFinished)
            }

            SocialLoginResult.Unavailable -> {
                socialLoginUseCase.onProviderUnavailable(socialType)
                dispatch(LoginReducerEvent.LoginFinished)
            }
        }
    }

    /**
     * 카카오 쪽 응답을 기다리는 중이면 기다림을 그만두고 다시 누를 수 있게 한다(늦게 온 결과는 무시된다).
     * 서버 검증이 시작된 뒤에는 로그인 상태가 곧 바뀌므로 막는다.
     */
    private fun abandonSocialLogin() {
        if (currentState.verifyingSocialLogin != null) return
        if (currentState.socialLoginInProgress == null) return
        dispatch(LoginReducerEvent.LoginFinished)
    }

    /**
     * debug 빌드에서 local.properties 로 주입한 테스트 계정으로 로그인한다.
     * release 는 자격 증명이 비어 있어 버튼이 노출되지 않으며, 인텐트가 들어와도 무시한다.
     */
    private fun testLogin() {
        if (!isTestLoginAvailable) return
        dispatch(LoginReducerEvent.TestLoginClicked)
        viewModelScope.launch {
            loginUseCase.login(
                email = BuildConfig.TEST_LOGIN_EMAIL,
                password = BuildConfig.TEST_LOGIN_PASSWORD,
            )
            dispatch(LoginReducerEvent.LoginFinished)
        }
    }
}

private val isTestLoginAvailable: Boolean =
    BuildConfig.TEST_LOGIN_EMAIL.isNotBlank() && BuildConfig.TEST_LOGIN_PASSWORD.isNotBlank()
