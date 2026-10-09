package com.dandi.nyummy.mailbox.presentation.write

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.mailbox.domain.SendInquiryUseCase
import com.dandi.nyummy.mailbox.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquiryWriteViewModel @Inject constructor(
    private val sendInquiry: SendInquiryUseCase,
    private val navigationHelper: NavigationHelper,
    private val messageHelper: MessageHelper,
) : MviViewModel<InquiryWriteIntent, InquiryWriteUIState, InquiryWriteReducerEvent>(
    InquiryWriteUIState.empty,
) {

    override fun onIntent(intent: InquiryWriteIntent) {
        when (intent) {
            InquiryWriteIntent.ClickBack -> navigationHelper.navigateToBack()
            InquiryWriteIntent.ClickCategory ->
                dispatch(InquiryWriteReducerEvent.CategorySheetVisibilityChanged(visible = true))
            is InquiryWriteIntent.SelectCategory ->
                dispatch(InquiryWriteReducerEvent.CategorySelected(intent.category))
            InquiryWriteIntent.DismissCategorySheet ->
                dispatch(InquiryWriteReducerEvent.CategorySheetVisibilityChanged(visible = false))
            is InquiryWriteIntent.ChangeContent ->
                dispatch(InquiryWriteReducerEvent.ContentChanged(intent.content))
            InquiryWriteIntent.ClickSend -> send()
        }
    }

    override fun reduce(state: InquiryWriteUIState, event: InquiryWriteReducerEvent): InquiryWriteUIState =
        when (event) {
            is InquiryWriteReducerEvent.CategorySheetVisibilityChanged ->
                state.copy(isCategorySheetVisible = event.visible)
            is InquiryWriteReducerEvent.CategorySelected ->
                state.copy(category = event.category, isCategorySheetVisible = false)
            is InquiryWriteReducerEvent.ContentChanged ->
                state.copy(content = event.content, isContentEmptyError = false)
            InquiryWriteReducerEvent.ContentEmptyRejected -> state.copy(isContentEmptyError = true)
            is InquiryWriteReducerEvent.SendingChanged -> state.copy(isSending = event.sending)
        }

    private fun send() {
        val state = currentState
        if (state.isSending) return
        if (state.content.isBlank()) {
            dispatch(InquiryWriteReducerEvent.ContentEmptyRejected)
            return
        }
        dispatch(InquiryWriteReducerEvent.SendingChanged(sending = true))
        viewModelScope.launch {
            sendInquiry(state.category, state.content)
                .onSuccess {
                    messageHelper.showSnackBar(messageRes = R.string.mailbox_write_sent)
                    navigationHelper.navigateToBack()
                }
                .onFailure { error ->
                    dispatch(InquiryWriteReducerEvent.SendingChanged(sending = false))
                    // 공통 오류(로그인 만료, 서버 문제)는 UseCase가 이미 다이얼로그로 알렸다.
                    if ((error as? HttpResponseException)?.isCommonErrorHandling() != true) {
                        messageHelper.showSnackBar(
                            iconType = IconType.ERROR,
                            messageRes = R.string.mailbox_write_send_failed,
                        )
                    }
                }
        }
    }
}
