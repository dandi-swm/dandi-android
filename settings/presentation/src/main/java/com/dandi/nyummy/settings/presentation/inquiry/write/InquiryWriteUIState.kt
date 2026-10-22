package com.dandi.nyummy.settings.presentation.inquiry.write

import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.settings.entity.InquiryCategory

/**
 * 문의하기 UI 상태.
 *
 * @property isTitleEmptyError 빈 제목으로 보내려 했다. 제목을 고치면 지운다.
 * @property isContentEmptyError 빈 내용으로 보내려 했다. 내용을 고치면 지운다.
 * @property imagePath 붙인 사진(앱 캐시의 JPEG) 경로. 사진이 없으면 null. 보낼 때 올린다.
 * @property isImageLoading 고른 사진을 첨부용으로 바꾸는 중. 그동안 보내기를 막는다.
 * @property isSending 보내는 중. 보내기 버튼을 막아 두 번 보내지 않게 한다.
 */
data class InquiryWriteUIState(
    val category: InquiryCategory = InquiryCategory.QUESTION,
    val title: String = "",
    val content: String = "",
    val isCategorySheetVisible: Boolean = false,
    val isTitleEmptyError: Boolean = false,
    val isContentEmptyError: Boolean = false,
    val imagePath: String? = null,
    val isImageLoading: Boolean = false,
    val isSending: Boolean = false,
) : UiState {

    companion object {
        val empty = InquiryWriteUIState()
    }
}
