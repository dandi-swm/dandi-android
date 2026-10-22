package com.dandi.nyummy.settings.presentation.inquiry.list

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.settings.entity.InquiryCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * 문의 목록 UI 상태.
 *
 * @property isLoaded 한 번이라도 문의 목록을 받았다. 받기 전에는 빈 화면을 보여 주지 않는다.
 * @property nextCursor 다음 묶음을 읽을 cursor. 마지막 묶음까지 받았으면 null.
 * @property isLoadingMore 다음 묶음을 읽는 중. 목록 끝에 로딩 표시를 둔다.
 */
data class InquiryListUIState(
    val inquiries: ImmutableList<InquiryRowUiModel> = persistentListOf(),
    val isLoaded: Boolean = false,
    val nextCursor: Long? = null,
    val isLoadingMore: Boolean = false,
) : UiState {

    val isEmpty: Boolean
        get() = isLoaded && inquiries.isEmpty()

    val hasNext: Boolean
        get() = nextCursor != null

    companion object {
        val empty = InquiryListUIState()
    }
}

/**
 * 문의 목록 한 행. 제목은 넘치면 말줄임한다.
 *
 * @property hasUnreadAnswer 답장이 달렸는데 아직 열어 보지 않았다. 행 앞 칸에 빨간 점을 단다.
 */
@Immutable
data class InquiryRowUiModel(
    val inquiryId: Long,
    val category: InquiryCategory,
    val title: String,
    val timeLabel: String,
    val isAnswered: Boolean,
    val hasUnreadAnswer: Boolean = false,
)
