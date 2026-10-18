package com.dandi.nyummy.mailbox.presentation.list

import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import kotlinx.collections.immutable.ImmutableList

sealed interface MailboxReducerEvent : ReducerEvent {
    /** 첫 묶음부터 다시 읽었다. 목록을 [inquiries]로 바꾼다. */
    data class InquiriesLoaded(
        val inquiries: ImmutableList<InquiryRowUiModel>,
        val nextCursor: Long?,
    ) : MailboxReducerEvent

    data object LoadMoreStarted : MailboxReducerEvent

    /** 다음 묶음을 받았다. 이미 있는 문의는 빼고 뒤에 이어 붙인다. */
    data class MoreInquiriesLoaded(
        val inquiries: ImmutableList<InquiryRowUiModel>,
        val nextCursor: Long?,
    ) : MailboxReducerEvent

    /** 다음 묶음 읽기가 실패했거나, 첫 묶음부터 다시 읽느라 멈췄다. */
    data object LoadMoreStopped : MailboxReducerEvent

    data class InquiryRemoved(val inquiryId: Long) : MailboxReducerEvent

    /** 되돌리기나 삭제 실패로 [row]를 원래 자리([index])에 다시 넣는다. */
    data class InquiryRestored(val row: InquiryRowUiModel, val index: Int) : MailboxReducerEvent

    data class TabSelected(val tab: MailboxTab) : MailboxReducerEvent

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
}
