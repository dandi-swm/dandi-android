package com.dandi.nyummy.mailbox.presentation.list

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.mailbox.domain.GetInquiriesUseCase
import com.dandi.nyummy.mailbox.domain.InquiryDetailPage
import com.dandi.nyummy.mailbox.domain.InquiryWritePage
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.mailbox.presentation.model.inquiryListTimeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val getInquiries: GetInquiriesUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MailboxIntent, MailboxUIState, MailboxReducerEvent>(MailboxUIState.empty) {

    private var loadJob: Job? = null

    override fun onIntent(intent: MailboxIntent) {
        when (intent) {
            MailboxIntent.ScreenResumed -> loadInquiries()
            MailboxIntent.ClickBack -> navigationHelper.navigateToBack()
            MailboxIntent.ClickWrite -> navigationHelper.navigateTo(InquiryWritePage)
            is MailboxIntent.ClickInquiry -> navigationHelper.navigateTo(InquiryDetailPage(intent.inquiryId))
        }
    }

    override fun reduce(state: MailboxUIState, event: MailboxReducerEvent): MailboxUIState =
        when (event) {
            is MailboxReducerEvent.InquiriesLoaded -> state.copy(inquiries = event.inquiries, isLoaded = true)
        }

    private fun loadInquiries() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // 실패하면 공통 처리(다이얼로그)는 UseCase가 하고, 화면은 받아 둔 목록을 그대로 둔다.
            getInquiries().onSuccess { inquiries ->
                val now = System.currentTimeMillis()
                dispatch(
                    MailboxReducerEvent.InquiriesLoaded(
                        inquiries.map { it.toRowUiModel(now) }.toImmutableList(),
                    ),
                )
            }
        }
    }
}

private fun InquiryVO.toRowUiModel(nowMillis: Long) = InquiryRowUiModel(
    inquiryId = inquiryId,
    category = category,
    title = content.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
    timeLabel = inquiryListTimeLabel(createdAtMillis, nowMillis),
    isAnswered = isAnswered,
)
