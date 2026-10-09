package com.dandi.nyummy.mailbox.presentation.write

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.mailbox.entity.InquiryCategory

/**
 * 문의하기 UI 상태.
 *
 * @property isContentEmptyError 빈 내용으로 보내려 했다. 내용을 고치면 지운다.
 * @property isSending 보내는 중. 보내기 버튼을 막아 두 번 보내지 않게 한다.
 */
data class InquiryWriteUIState(
    val category: InquiryCategory = InquiryCategory.QUESTION,
    val content: String = "",
    val isCategorySheetVisible: Boolean = false,
    val isContentEmptyError: Boolean = false,
    val isSending: Boolean = false,
) : UiState {

    companion object {
        val empty = InquiryWriteUIState()
    }
}
