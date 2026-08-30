package com.dandi.nyummy.auth.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.EmailLoginPage
import com.dandi.nyummy.auth.domain.LoginUseCase
import com.dandi.nyummy.auth.entity.SocialLoginType
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    val navigationHelper: NavigationHelper,
    val loginUseCase: LoginUseCase
) :
    MviViewModel<LoginIntent, LoginUIState, LoginReducerEvent>(LoginUIState.empty) {
    override fun onIntent(intent: LoginIntent) {

        when (intent) {
            is LoginIntent.ClickSocialLogin -> socialLogin(intent.socialType)
            LoginIntent.ClickEmailLogin -> emailLogin()
            LoginIntent.ClickTestLogin -> {
                testLogin()
            }
        }
    }

    override fun reduce(
        state: LoginUIState,
        event: LoginReducerEvent
    ): LoginUIState {
        return when (event) {
            is LoginReducerEvent.SocialLoginStarted -> state.copy(isLoading = true)
            is LoginReducerEvent.LoginFinished -> state.copy(isLoading = false)
            is LoginReducerEvent.EmailLoginClicked -> state.copy(isLoading = false)
            is LoginReducerEvent.TestLoginClicked -> state.copy(isLoading = true)
        }
    }

    private fun emailLogin() {
        dispatch(LoginReducerEvent.EmailLoginClicked)
        navigationHelper.navigateTo(EmailLoginPage)
    }
    private fun socialLogin(socialType: SocialLoginType) {
        dispatch(LoginReducerEvent.SocialLoginStarted(socialType))
        // TODO: 소셜 로그인 UseCase 호출 후 성공 시 홈 이동, 실패 다이얼로그 모두 UseCase 가 처리한다.
        dispatch(LoginReducerEvent.LoginFinished)
    }

    // TODO: 추후에 제거해야함
    private fun testLogin() {
        dispatch(LoginReducerEvent.TestLoginClicked)
        viewModelScope.launch {
            loginUseCase.login(email = "test@dandi.com", password = "Test1234!")
            dispatch(LoginReducerEvent.LoginFinished)
        }
    }





}
