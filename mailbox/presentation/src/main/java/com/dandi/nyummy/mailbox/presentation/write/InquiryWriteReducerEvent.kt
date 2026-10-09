package com.dandi.nyummy.mailbox.presentation.write

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.mailbox.entity.InquiryCategory

sealed interface InquiryWriteReducerEvent : ReducerEvent {
    data class CategorySheetVisibilityChanged(val visible: Boolean) : InquiryWriteReducerEvent
    data class CategorySelected(val category: InquiryCategory) : InquiryWriteReducerEvent
    data class ContentChanged(val content: String) : InquiryWriteReducerEvent
    data object ContentEmptyRejected : InquiryWriteReducerEvent
    data class SendingChanged(val sending: Boolean) : InquiryWriteReducerEvent
}
