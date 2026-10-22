package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.settings.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.settings.data.dto.InquiryDTO
import com.dandi.nyummy.settings.data.dto.InquiryListResponseDTO

class InquiryRemoteDataSource(
    private val apiService: InquiryApiService,
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
