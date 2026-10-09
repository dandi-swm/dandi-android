package com.dandi.nyummy.mailbox.presentation.detail

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 문의 상세에서 발생하는 사용자 의도. */
sealed interface InquiryDetailIntent : MviIntent {

    /** 화면에 들어왔다. [inquiryId] 문의를 읽는다. */
    data class Enter(val inquiryId: Long) : InquiryDetailIntent

    data object ClickBack : InquiryDetailIntent
}
