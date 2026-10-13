package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.mailbox.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import com.dandi.nyummy.mailbox.entity.InquiryCategory

class MailboxRepositoryImpl(
    private val dataSource: MailboxDataSource,
) : MailboxRepository {

    override suspend fun getInquiries(cursor: Long?, size: Int) =
        dataSource.getInquiries(cursor, size).toVO()

    override suspend fun getInquiry(inquiryId: Long) =
        dataSource.getInquiry(inquiryId).toVO()

    override suspend fun sendInquiry(category: InquiryCategory, title: String, content: String) =
        dataSource.createInquiry(
            CreateInquiryRequestDTO(category = category.name, questionTitle = title, questionContent = content),
        )

    override suspend fun deleteInquiry(inquiryId: Long) =
        dataSource.deleteInquiry(inquiryId)

    override suspend fun restoreInquiry(inquiryId: Long) =
        dataSource.restoreInquiry(inquiryId)
}
