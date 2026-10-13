package com.dandi.nyummy.mailbox.presentation.write

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.mailbox.entity.InquiryCategory

sealed interface InquiryWriteReducerEvent : ReducerEvent {
    data class CategorySheetVisibilityChanged(val visible: Boolean) : InquiryWriteReducerEvent
    data class CategorySelected(val category: InquiryCategory) : InquiryWriteReducerEvent
    data class TitleChanged(val title: String) : InquiryWriteReducerEvent
    data class ContentChanged(val content: String) : InquiryWriteReducerEvent

    /** 빈 칸이 있어 보내지 않았다. 빈 칸마다 오류를 띄운다. */
    data class EmptyInputRejected(val isTitleEmpty: Boolean, val isContentEmpty: Boolean) : InquiryWriteReducerEvent
    data class SendingChanged(val sending: Boolean) : InquiryWriteReducerEvent
}
