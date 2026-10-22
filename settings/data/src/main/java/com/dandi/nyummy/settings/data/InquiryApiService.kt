package com.dandi.nyummy.settings.data

import com.dandi.nyummy.settings.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.settings.data.dto.InquiryDTO
import com.dandi.nyummy.settings.data.dto.InquiryListResponseDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 문의 API
 */
interface InquiryApiService {
    /** 문의 목록 조회 */
    @GET(INQUIRIES_PATH)
    suspend fun getInquiries(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): Response<InquiryListResponseDTO>

    /** 문의 상세 조회 */
    @GET("$INQUIRIES_PATH/{inquiryId}")
    suspend fun getInquiry(@Path("inquiryId") inquiryId: Long): Response<InquiryDTO>

    /** 문의하기 */
    @POST(INQUIRIES_PATH)
    suspend fun createInquiry(@Body request: CreateInquiryRequestDTO): Response<Unit>

    /** 문의 삭제 */
    @DELETE("$INQUIRIES_PATH/{inquiryId}")
    suspend fun deleteInquiry(@Path("inquiryId") inquiryId: Long): Response<Unit>

    /** 문의 복구 (서버 추가 요청 중) */
    @POST("$INQUIRIES_PATH/{inquiryId}/restore")
    suspend fun restoreInquiry(@Path("inquiryId") inquiryId: Long): Response<Unit>

    companion object {
        const val INQUIRIES_PATH = "/api/v1/inquiries"
    }
}
