package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import kotlinx.collections.immutable.ImmutableList

sealed interface MailboxReducerEvent : ReducerEvent {
    /** 우편 첫 묶음부터 다시 읽었다. 목록을 [mails]로 바꾼다. */
    data class MailsLoaded(
        val mails: ImmutableList<MailRowUiModel>,
        val nextCursor: Long?,
    ) : MailboxReducerEvent

    data object MailLoadMoreStarted : MailboxReducerEvent

    /** 우편 다음 묶음을 받았다. 이미 있는 우편은 빼고 뒤에 이어 붙인다. */
    data class MoreMailsLoaded(
        val mails: ImmutableList<MailRowUiModel>,
        val nextCursor: Long?,
    ) : MailboxReducerEvent

    data object MailLoadMoreStopped : MailboxReducerEvent

    data class MailClaimStarted(val mailId: Long) : MailboxReducerEvent

    /** 받기가 끝났다. [isClaimed]면 그 행을 받았어요로 바꾸고, 아니면(실패) 버튼만 다시 살린다. */
    data class MailClaimFinished(val mailId: Long, val isClaimed: Boolean) : MailboxReducerEvent

    data object ClaimAllStarted : MailboxReducerEvent

    /** 모두 받기가 끝났다. [isClaimed]면 불러온 우편을 모두 받았어요로 바꾼다. */
    data class ClaimAllFinished(val isClaimed: Boolean) : MailboxReducerEvent

    data class MailRemoved(val mailId: Long) : MailboxReducerEvent

    /** 되돌리기나 삭제 실패로 [row]를 원래 자리([index])에 다시 넣는다. */
    data class MailRestored(val row: MailRowUiModel, val index: Int) : MailboxReducerEvent

    /** 상자 열기 연출의 변화. 우편 목록은 바꾸지 않는다. */
    sealed interface BoxOpeningEvent : MailboxReducerEvent

    /** 상자를 열기 시작했다. 닫힌 상자가 무대에 나와 응답을 기다린다. */
    data class BoxOpeningStarted(val opening: BoxOpeningUiState) : BoxOpeningEvent

    /** 받은 보상이 왔다. 응답의 상자 번호로 그림을 다시 정한다. */
    data class BoxOpeningOpened(
        val closedImageUrl: String,
        val openedImageUrl: String,
        val rewards: ImmutableList<BoxRewardUiModel>,
    ) : BoxOpeningEvent

    data object BoxOpeningRevealed : BoxOpeningEvent

    data class BoxOpeningFailed(val failure: BoxOpeningFailure) : BoxOpeningEvent

    data object BoxOpeningEnded : BoxOpeningEvent
}
