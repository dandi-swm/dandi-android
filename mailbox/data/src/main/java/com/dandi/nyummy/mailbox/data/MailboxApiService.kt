package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.mailbox.data.dto.MailListResponseDTO
import com.dandi.nyummy.mailbox.data.dto.RewardDTO
import com.dandi.nyummy.mailbox.data.dto.TotalRewardDTO
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 우편함 API
 */
interface MailboxApiService {
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
        const val MAILS_PATH = "/api/v1/mails"
    }
}
