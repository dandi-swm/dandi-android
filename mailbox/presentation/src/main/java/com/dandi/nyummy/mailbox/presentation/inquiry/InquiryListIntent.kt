package com.dandi.nyummy.mailbox.presentation.inquiry

import com.dandi.nyummy.common.presentation.mvi.MviIntent

/** 문의 목록에서 발생하는 사용자 의도. */
sealed interface InquiryListIntent : MviIntent {

    /** 화면이 보이게 됐다(첫 진입, 문의를 보내거나 상세를 보고 복귀). 목록을 다시 읽는다. */
    data object ScreenResumed : InquiryListIntent

    data object ClickBack : InquiryListIntent

    /** 목록 끝에 가까워졌다. 다음 묶음이 있으면 읽는다. */
    data object ReachListEnd : InquiryListIntent

    /** 상단 바나 빈 화면의 문의하기. */
    data object ClickWrite : InquiryListIntent

    data class ClickInquiry(val inquiryId: Long) : InquiryListIntent

    /** 행을 왼쪽으로 밀어 지웠다. 목록에서 바로 빼고 서버에서 지운다(소프트 삭제). */
    data class DeleteInquiry(val inquiryId: Long) : InquiryListIntent

    /** 지운 뒤 스낵바의 되돌리기. [row]를 원래 자리([index])에 되살린다. */
    data class UndoDelete(val row: InquiryRowUiModel, val index: Int) : InquiryListIntent
}
