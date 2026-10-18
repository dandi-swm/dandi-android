package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.mailbox.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.mailbox.data.dto.InquiryDTO
import com.dandi.nyummy.mailbox.data.dto.InquiryListResponseDTO
import com.dandi.nyummy.mailbox.data.dto.MailListResponseDTO
import com.dandi.nyummy.mailbox.data.dto.RewardDTO
import com.dandi.nyummy.mailbox.data.dto.TotalRewardDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 우편함 API(아이템 탭의 우편, 문의)
 */
interface MailboxApiService {
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

    /** 우편 목록 조회 */
    @GET(MAILS_PATH)
    suspend fun getMails(
        @Query("cursor") cursor: Long?,
        @Query("size") size: Int,
    ): Response<MailListResponseDTO>

    /** 우편 하나 받기 */
    @POST("$MAILS_PATH/{mailId}/claim")
    suspend fun claimMail(@Path("mailId") mailId: Long): Response<RewardDTO>

    /** 우편 모두 받기 */
    @POST("$MAILS_PATH/claim-all")
    suspend fun claimAllMails(): Response<TotalRewardDTO>

    /** 받은 우편 삭제 */
    @DELETE("$MAILS_PATH/{mailId}")
    suspend fun deleteMail(@Path("mailId") mailId: Long): Response<Unit>

    /** 우편 복구 */
    @POST("$MAILS_PATH/{mailId}/restore")
    suspend fun restoreMail(@Path("mailId") mailId: Long): Response<Unit>

    companion object {
        const val INQUIRIES_PATH = "/api/v1/inquiries"
        const val MAILS_PATH = "/api/v1/mails"
    }
}
