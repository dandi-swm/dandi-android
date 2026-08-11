package com.dandi.nyummy.auth.presentation

import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmailLoginViewModel @Inject constructor(
    val navigationHelper: NavigationHelper,
    val messageHelper: MessageHelper
) : MviViewModel<EmailLoginIntent, EmailLoginUIState, EmailLoginReducerEvent>(EmailLoginUIState.empty) {

    override fun onIntent(intent: EmailLoginIntent) {
        when (intent) {
            is EmailLoginIntent.InputEmail -> dispatch(EmailLoginReducerEvent.EmailChanged(intent.value))
            is EmailLoginIntent.InputPassword -> dispatch(EmailLoginReducerEvent.PasswordChanged(intent.value))
            EmailLoginIntent.ClickLogin -> login()
            EmailLoginIntent.ClickForgotPassword -> forgotPassword()
            EmailLoginIntent.ClickSignUp -> signUp()
        }
    }

    override fun reduce(state: EmailLoginUIState, event: EmailLoginReducerEvent): EmailLoginUIState =
        when (event) {
            is EmailLoginReducerEvent.EmailChanged -> state.copy(email = event.value)
            is EmailLoginReducerEvent.PasswordChanged -> state.copy(password = event.value)
            EmailLoginReducerEvent.LoginStarted -> state.copy(isLoading = true)
            EmailLoginReducerEvent.LoginFinished -> state.copy(isLoading = false)
        }

    private fun login() {
        dispatch(EmailLoginReducerEvent.LoginStarted)
        // TODO: 이메일 로그인 UseCase 호출 후 성공 시 홈으로 이동
        dispatch(EmailLoginReducerEvent.LoginFinished)
    }

    private fun forgotPassword() {
        // TODO: 비밀번호 찾기 화면 미구현 — Page 추가 후 navigationHelper.navigateTo(...)
    }

    private fun signUp() {
        // TODO: 회원가입 화면 미구현 — Page 추가 후 navigationHelper.navigateTo(...)
    }
}
