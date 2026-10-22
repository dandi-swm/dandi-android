package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.mailbox.entity.RewardVO

interface MailboxRepository {

    /** 아이템 탭의 우편 목록 한 묶음(최신순). 첫 묶음은 [cursor] 없이, 다음부터는 직전 묶음의 nextCursor로 읽는다. */
    suspend fun getMails(cursor: Long?, size: Int): MailPageVO

    /** 우편 하나를 받고 지급된 보상을 돌려준다. 보상 상자도 이걸로 연다. */
    suspend fun claimMail(mailId: Long): RewardVO

    /** 안 받은 우편을 모두 받고 보상의 합계를 돌려준다. 보상 상자는 연출 없이 합계에 더해진다. */
    suspend fun claimAllMails(): RewardVO

    /** 받은 우편을 지운다(서버는 삭제 표시만 해 되살릴 수 있다). 안 받은 우편은 지울 수 없다. */
    suspend fun deleteMail(mailId: Long)

    /** 지운 우편을 되살린다. */
    suspend fun restoreMail(mailId: Long)
}
