package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.InquiryPageVO
import com.dandi.nyummy.mailbox.entity.InquiryVO

interface MailboxRepository {

    /** 내가 보낸 문의 목록 한 묶음(최신순). 첫 묶음은 [cursor] 없이, 다음부터는 직전 묶음의 nextCursor로 읽는다. */
    suspend fun getInquiries(cursor: Long?, size: Int): InquiryPageVO

    /** 문의 하나. 없으면 null. */
    suspend fun getInquiry(inquiryId: Long): InquiryVO?

    /** 문의를 보낸다. 서버는 만든 문의를 돌려주지 않는다(본문 없는 201). */
    suspend fun sendInquiry(category: InquiryCategory, title: String, content: String)

    /** 문의를 우편함에서 지운다(서버는 소프트 삭제). */
    suspend fun deleteInquiry(inquiryId: Long)

    /** 지운 문의를 되살린다. */
    suspend fun restoreInquiry(inquiryId: Long)
}
