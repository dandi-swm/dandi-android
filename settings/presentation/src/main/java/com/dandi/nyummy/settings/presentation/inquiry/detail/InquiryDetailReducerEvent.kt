package com.dandi.nyummy.settings.presentation.inquiry.detail

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent

sealed interface InquiryDetailReducerEvent : ReducerEvent {
    data class InquiryLoaded(val inquiry: InquiryDetailUiModel) : InquiryDetailReducerEvent
}
