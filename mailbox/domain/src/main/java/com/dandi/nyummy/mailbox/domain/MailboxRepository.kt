package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.mailbox.entity.InquiryVO

interface MailboxRepository {

    /** 내가 보낸 문의 목록. 최근에 보낸 문의가 앞에 온다. */
    suspend fun getInquiries(): List<InquiryVO>

    /** 문의 하나. 없으면 null. */
    suspend fun getInquiry(inquiryId: Long): InquiryVO?

    /** 문의를 보내고, 만들어진 문의를 돌려준다. */
    suspend fun sendInquiry(category: InquiryCategory, content: String): InquiryVO
}
