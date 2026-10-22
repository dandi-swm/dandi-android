package com.dandi.nyummy.mailbox.presentation.write

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.mailbox.domain.AttachInquiryImageUseCase
import com.dandi.nyummy.mailbox.domain.SendInquiryUseCase
import com.dandi.nyummy.mailbox.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquiryWriteViewModel @Inject constructor(
    private val sendInquiry: SendInquiryUseCase,
    private val attachInquiryImage: AttachInquiryImageUseCase,
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
            is InquiryWriteIntent.ChangeTitle ->
                dispatch(InquiryWriteReducerEvent.TitleChanged(intent.title))
            is InquiryWriteIntent.ChangeContent ->
                dispatch(InquiryWriteReducerEvent.ContentChanged(intent.content))
            is InquiryWriteIntent.ImagePicked -> intent.photoUri?.let(::attachImage)
            InquiryWriteIntent.ClickRemoveImage -> dispatch(InquiryWriteReducerEvent.ImageRemoved)
            InquiryWriteIntent.ClickSend -> send()
        }
    }

    override fun reduce(state: InquiryWriteUIState, event: InquiryWriteReducerEvent): InquiryWriteUIState =
        when (event) {
            is InquiryWriteReducerEvent.CategorySheetVisibilityChanged ->
                state.copy(isCategorySheetVisible = event.visible)
            is InquiryWriteReducerEvent.CategorySelected ->
                state.copy(category = event.category, isCategorySheetVisible = false)
            is InquiryWriteReducerEvent.TitleChanged ->
                state.copy(title = event.title, isTitleEmptyError = false)
            is InquiryWriteReducerEvent.ContentChanged ->
                state.copy(content = event.content, isContentEmptyError = false)
            is InquiryWriteReducerEvent.EmptyInputRejected ->
                state.copy(isTitleEmptyError = event.isTitleEmpty, isContentEmptyError = event.isContentEmpty)
            is InquiryWriteReducerEvent.SendingChanged -> state.copy(isSending = event.sending)
            is InquiryWriteReducerEvent.ImageLoadingChanged -> state.copy(isImageLoading = event.loading)
            is InquiryWriteReducerEvent.ImageAttached ->
                state.copy(imagePath = event.imagePath, isImageLoading = false)
            InquiryWriteReducerEvent.ImageRemoved -> state.copy(imagePath = null)
        }

    private fun attachImage(photoUri: String) {
        if (currentState.isImageLoading) return
        dispatch(InquiryWriteReducerEvent.ImageLoadingChanged(loading = true))
        viewModelScope.launch {
            attachInquiryImage(photoUri)
                .onSuccess { dispatch(InquiryWriteReducerEvent.ImageAttached(it)) }
                .onFailure {
                    dispatch(InquiryWriteReducerEvent.ImageLoadingChanged(loading = false))
                    messageHelper.showToast(R.string.mailbox_write_image_load_failed)
                }
        }
    }

    private fun send() {
        val state = currentState
        if (state.isSending || state.isImageLoading) return
        val isTitleEmpty = state.title.isBlank()
        val isContentEmpty = state.content.isBlank()
        if (isTitleEmpty || isContentEmpty) {
            dispatch(InquiryWriteReducerEvent.EmptyInputRejected(isTitleEmpty, isContentEmpty))
            return
        }
        dispatch(InquiryWriteReducerEvent.SendingChanged(sending = true))
        viewModelScope.launch {
            sendInquiry(state.category, state.title, state.content, state.imagePath)
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
