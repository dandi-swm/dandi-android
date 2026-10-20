package com.dandi.nyummy.mailbox.presentation.list

import androidx.compose.runtime.Immutable
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.mailbox.entity.MailCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

/**
 * 우편함 UI 상태. 받은 우편(보상, 코인, 아이템)만 보여 준다. 문의는 설정의 문의하기로 옮겼다.
 *
 * @property items 받은 우편 목록 상태.
 */
data class MailboxUIState(
    val items: MailItemsUiState = MailItemsUiState(),
) : UiState {

    companion object {
        val empty = MailboxUIState()
    }
}

/**
 * 받은 우편 목록 상태.
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
 * 우편 한 행.
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
