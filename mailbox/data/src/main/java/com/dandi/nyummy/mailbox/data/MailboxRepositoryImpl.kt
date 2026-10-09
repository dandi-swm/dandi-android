package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.mailbox.data.dto.SendInquiryRequestDTO
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import com.dandi.nyummy.mailbox.entity.InquiryCategory

class MailboxRepositoryImpl(
    private val dataSource: MailboxDataSource,
) : MailboxRepository {

    override suspend fun getInquiries() =
        dataSource.getInquiries().toVO()

    override suspend fun getInquiry(inquiryId: Long) =
        dataSource.getInquiry(inquiryId)?.toVO()

    override suspend fun sendInquiry(category: InquiryCategory, content: String) =
        dataSource.sendInquiry(SendInquiryRequestDTO(category = category.name, content = content)).toVO()
}
