package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.common.data.BaseRemoteDataSource
import com.dandi.nyummy.mailbox.data.dto.MailListResponseDTO
import com.dandi.nyummy.mailbox.data.dto.RewardDTO
import com.dandi.nyummy.mailbox.data.dto.TotalRewardDTO

class MailboxDataSource(
    private val apiService: MailboxApiService,
) : BaseRemoteDataSource() {
    suspend fun getMails(cursor: Long?, size: Int): MailListResponseDTO =
        checkResponse(apiService.getMails(cursor, size))

    suspend fun claimMail(mailId: Long): RewardDTO =
        checkResponse(apiService.claimMail(mailId))

    suspend fun claimAllMails(): TotalRewardDTO =
        checkResponse(apiService.claimAllMails())

    suspend fun deleteMail(mailId: Long) =
        checkResponse(apiService.deleteMail(mailId))

    suspend fun restoreMail(mailId: Long) =
        checkResponse(apiService.restoreMail(mailId))
}
