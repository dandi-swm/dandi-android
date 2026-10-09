package com.dandi.nyummy.mailbox.presentation.detail

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.mailbox.domain.GetInquiryDetailUseCase
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.mailbox.presentation.model.inquiryDetailTimeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquiryDetailViewModel @Inject constructor(
    private val getInquiryDetail: GetInquiryDetailUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<InquiryDetailIntent, InquiryDetailUIState, InquiryDetailReducerEvent>(
    InquiryDetailUIState.empty,
) {

    private var loadedInquiryId: Long? = null

    override fun onIntent(intent: InquiryDetailIntent) {
        when (intent) {
            is InquiryDetailIntent.Enter -> load(intent.inquiryId)
            InquiryDetailIntent.ClickBack -> navigationHelper.navigateToBack()
        }
    }

    override fun reduce(state: InquiryDetailUIState, event: InquiryDetailReducerEvent): InquiryDetailUIState =
        when (event) {
            is InquiryDetailReducerEvent.InquiryLoaded -> state.copy(inquiry = event.inquiry)
        }

    private fun load(inquiryId: Long) {
        // 화면이 다시 그려져도 같은 문의는 한 번만 읽는다.
        if (loadedInquiryId == inquiryId) return
        loadedInquiryId = inquiryId
        viewModelScope.launch {
            getInquiryDetail(inquiryId)
                .onSuccess { inquiry ->
                    // 지워졌거나 잘못된 링크로 들어온 문의는 보여 줄 것이 없어 우편함으로 돌아간다.
                    if (inquiry == null) {
                        navigationHelper.navigateToBack()
                    } else {
                        dispatch(InquiryDetailReducerEvent.InquiryLoaded(inquiry.toDetailUiModel()))
                    }
                }
                .onFailure { loadedInquiryId = null }
        }
    }
}

private fun InquiryVO.toDetailUiModel() = InquiryDetailUiModel(
    category = category,
    content = content,
    createdAtLabel = inquiryDetailTimeLabel(createdAtMillis),
    answer = if (isAnswered) {
        InquiryAnswerUiModel(
            content = answer.content,
            answeredAtLabel = inquiryDetailTimeLabel(answer.answeredAtMillis),
        )
    } else {
        null
    },
)
