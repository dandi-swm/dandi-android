package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.MailCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 우편함 UI 상태. 아이템 탭이 켜져 있으면 아이템 / 문의 두 탭이고, 꺼져 있으면 문의 목록만 보인다.
 *
 * @property isLoaded 한 번이라도 문의 목록을 받았다. 받기 전에는 빈 화면을 보여 주지 않는다.
 * @property nextCursor 문의 다음 묶음을 읽을 cursor. 마지막 묶음까지 받았으면 null.
 * @property isLoadingMore 문의 다음 묶음을 읽는 중. 목록 끝에 로딩 표시를 둔다.
 * @property isItemTabEnabled 아이템 탭을 보여 준다(원격 구성 기능 플래그).
 * @property selectedTab 보고 있는 탭. 아이템 탭이 꺼져 있으면 늘 문의다.
 * @property items 아이템 탭(받은 우편) 상태.
 */
data class MailboxUIState(
    val inquiries: ImmutableList<InquiryRowUiModel> = persistentListOf(),
    val isLoaded: Boolean = false,
    val nextCursor: Long? = null,
    val isLoadingMore: Boolean = false,
    val isItemTabEnabled: Boolean = false,
    val selectedTab: MailboxTab = MailboxTab.INQUIRY,
    val items: MailItemsUiState = MailItemsUiState(),
) : UiState {

    val isEmpty: Boolean
        get() = isLoaded && inquiries.isEmpty()

    val hasNext: Boolean
        get() = nextCursor != null

    companion object {
        val empty = MailboxUIState()

        /** 아이템 탭이 켜져 있으면 아이템 탭부터 보여 준다. */
        fun initial(isItemTabEnabled: Boolean): MailboxUIState = MailboxUIState(
            isItemTabEnabled = isItemTabEnabled,
            selectedTab = if (isItemTabEnabled) MailboxTab.ITEM else MailboxTab.INQUIRY,
        )
    }
}

/** 우편함 탭. 화면에 보이는 순서와 같다. */
enum class MailboxTab {
    ITEM,
    INQUIRY,
}

/**
 * 아이템 탭(받은 우편) 상태.
 *
 * @property isLoaded 한 번이라도 우편 목록을 받았다. 받기 전에는 빈 화면을 보여 주지 않는다.
 * @property nextCursor 다음 묶음을 읽을 cursor. 마지막 묶음까지 받았으면 null.
 * @property claimingMailIds 받기 요청이 진행 중인 우편. 버튼을 막아 두 번 받지 않게 한다.
 * @property isClaimingAll 모두 받기 요청이 진행 중.
 */
@Immutable
data class MailItemsUiState(
    val mails: ImmutableList<MailRowUiModel> = persistentListOf(),
    val isLoaded: Boolean = false,
    val nextCursor: Long? = null,
    val isLoadingMore: Boolean = false,
    val claimingMailIds: ImmutableSet<Long> = persistentSetOf(),
    val isClaimingAll: Boolean = false,
) {
    val isEmpty: Boolean
        get() = isLoaded && mails.isEmpty()

    /** 불러온 우편 중 안 받은 게 있다. 모두 받기 버튼을 보여 준다. */
    val hasUnclaimed: Boolean
        get() = mails.any { !it.isClaimed }

    fun isClaiming(mailId: Long): Boolean = isClaimingAll || mailId in claimingMailIds
}

/**
 * 아이템 탭 한 행.
 *
 * @property content 보낸 이유나 받은 내용(예 "첫 기록 보상", "상점에서 샀어요").
 * @property remainingDays 받지 않으면 사라질 때까지 남은 날. 받은 우편이거나 사라지지 않는 우편이면 null.
 */
@Immutable
data class MailRowUiModel(
    val mailId: Long,
    val category: MailCategory,
    val title: String,
    val content: String,
    val imageUrl: String,
    val timeLabel: String,
    val remainingDays: Int?,
    val isClaimed: Boolean,
)

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
