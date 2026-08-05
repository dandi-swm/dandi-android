package com.dandi.nyummy.auth.presentation

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.auth.domain.LoginUseCase
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmailLoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : MviViewModel<EmailLoginIntent, EmailLoginUIState, EmailLoginReducerEvent>(EmailLoginUIState.empty) {

    override fun onIntent(intent: EmailLoginIntent) {
        when (intent) {
            is EmailLoginIntent.EmailChanged ->
                dispatch(EmailLoginReducerEvent.EmailChanged(intent.email))

            is EmailLoginIntent.PasswordChanged ->
                dispatch(EmailLoginReducerEvent.PasswordChanged(intent.password))

            EmailLoginIntent.ClickLogin -> login()
        }
    }

    override fun reduce(state: EmailLoginUIState, event: EmailLoginReducerEvent): EmailLoginUIState =
        when (event) {
            is EmailLoginReducerEvent.EmailChanged -> state.copy(email = event.email)
            is EmailLoginReducerEvent.PasswordChanged -> state.copy(password = event.password)
            EmailLoginReducerEvent.LoginStarted -> state.copy(isLoading = true)
            EmailLoginReducerEvent.LoginFinished -> state.copy(isLoading = false)
        }

    private fun login() {
        if (!currentState.isLoginEnabled) return
        dispatch(EmailLoginReducerEvent.LoginStarted)
        viewModelScope.launch {
            // 성공 시 홈 이동, 실패 다이얼로그 모두 UseCase 가 처리한다.
            loginUseCase.login(email = currentState.email, password = currentState.password)
            dispatch(EmailLoginReducerEvent.LoginFinished)
        }
    }
}
