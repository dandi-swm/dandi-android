package com.dandi.nyummy.mailbox.presentation.detail

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface InquiryDetailReducerEvent : ReducerEvent {
    data class InquiryLoaded(val inquiry: InquiryDetailUiModel) : InquiryDetailReducerEvent
}
