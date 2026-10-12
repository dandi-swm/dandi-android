package com.dandi.nyummy.mailbox.presentation.detail

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.mailbox.entity.InquiryCategory

/**
 * 문의 상세 UI 상태. [inquiry]가 null이면 아직 읽는 중이다.
 */
data class InquiryDetailUIState(
    val inquiry: InquiryDetailUiModel? = null,
) : UiState {

    companion object {
        val empty = InquiryDetailUIState()
    }
}

/**
 * 상세에 보여 줄 문의.
 *
 * @property answer 답장. 아직 답장이 없으면 null이고, 화면은 내가 보낸 문의만 보여 준다.
 */
@Immutable
data class InquiryDetailUiModel(
    val category: InquiryCategory,
    val content: String,
    val createdAtLabel: String,
    val answer: InquiryAnswerUiModel?,
)

@Immutable
data class InquiryAnswerUiModel(
    val content: String,
    val answeredAtLabel: String,
)
