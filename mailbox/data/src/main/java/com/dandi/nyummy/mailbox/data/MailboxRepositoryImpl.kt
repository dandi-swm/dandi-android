package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.mailbox.domain.MailboxRepository

class MailboxRepositoryImpl(
    private val dataSource: MailboxDataSource,
) : MailboxRepository {

    override suspend fun getMails(cursor: Long?, size: Int) =
        dataSource.getMails(cursor, size).toVO()

    override suspend fun claimMail(mailId: Long) =
        dataSource.claimMail(mailId).toVO()

    override suspend fun claimAllMails() =
        dataSource.claimAllMails().toVO()

    override suspend fun deleteMail(mailId: Long) =
        dataSource.deleteMail(mailId)

    override suspend fun restoreMail(mailId: Long) =
        dataSource.restoreMail(mailId)
}
