package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.mailbox.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.mailbox.data.dto.InquiryDTO
import com.dandi.nyummy.mailbox.data.dto.InquiryListResponseDTO

class MailboxDataSource(
    private val apiService: MailboxApiService,
) : BaseRemoteDataSource() {
    suspend fun getInquiries(cursor: Long?, size: Int): InquiryListResponseDTO =
        checkResponse(apiService.getInquiries(cursor, size))

    suspend fun getInquiry(inquiryId: Long): InquiryDTO =
        checkResponse(apiService.getInquiry(inquiryId))

    suspend fun createInquiry(request: CreateInquiryRequestDTO) =
        checkResponse(apiService.createInquiry(request))

    suspend fun deleteInquiry(inquiryId: Long) =
        checkResponse(apiService.deleteInquiry(inquiryId))

    suspend fun restoreInquiry(inquiryId: Long) =
        checkResponse(apiService.restoreInquiry(inquiryId))
}
