package com.dandi.nyummy.settings.presentation.inquiry.write

import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.settings.entity.InquiryCategory

/** 문의하기 화면에서 발생하는 사용자 의도. */
sealed interface InquiryWriteIntent : MviIntent {

    data object ClickBack : InquiryWriteIntent

    /** 문의 유형 칸을 눌렀다. 유형 시트를 연다. */
    data object ClickCategory : InquiryWriteIntent

    /** 시트에서 유형을 골랐다. 고르면 시트가 닫힌다. */
    data class SelectCategory(val category: InquiryCategory) : InquiryWriteIntent

    data object DismissCategorySheet : InquiryWriteIntent

    data class ChangeTitle(val title: String) : InquiryWriteIntent

    data class ChangeContent(val content: String) : InquiryWriteIntent

    /** 사진 선택기가 닫혔다. 고르지 않고 닫았으면 [photoUri]는 null. */
    data class ImagePicked(val photoUri: String?) : InquiryWriteIntent

    data object ClickRemoveImage : InquiryWriteIntent

    data object ClickSend : InquiryWriteIntent
}
