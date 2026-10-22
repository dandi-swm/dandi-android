package com.dandi.nyummy.mailbox.data

import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import com.dandi.nyummy.common.data.image.ImageUploadPurpose
import com.dandi.nyummy.mailbox.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.mailbox.data.image.InquiryImageDataSource
import com.dandi.nyummy.mailbox.domain.MailboxRepository
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MailboxRepositoryImpl(
    private val dataSource: MailboxDataSource,
    private val imageUploadDataSource: ImageUploadDataSource,
    private val inquiryImageDataSource: InquiryImageDataSource,
) : MailboxRepository {

    override suspend fun getInquiries(cursor: Long?, size: Int) =
        dataSource.getInquiries(cursor, size).toVO()

    override suspend fun getInquiry(inquiryId: Long) =
        dataSource.getInquiry(inquiryId).toVO()

    override suspend fun prepareInquiryImage(photoUri: String) = withContext(Dispatchers.IO) {
        inquiryImageDataSource.prepare(photoUri)
    }

    override suspend fun uploadInquiryImage(imagePath: String): String {
        val file = File(imagePath)
        val target = imageUploadDataSource.issueUploadUrl(
            purpose = ImageUploadPurpose.INQUIRY,
            contentType = IMAGE_CONTENT_TYPE,
            fileSizeBytes = file.length(),
        )
        imageUploadDataSource.upload(
            uploadUrl = target.uploadUrl.orEmpty(),
            uploadHeaders = target.uploadHeaders.orEmpty(),
            file = file,
            defaultContentType = IMAGE_CONTENT_TYPE,
        )
        return target.imageKey.orEmpty()
    }

    override suspend fun sendInquiry(category: InquiryCategory, title: String, content: String, imageKey: String?) =
        dataSource.createInquiry(
            CreateInquiryRequestDTO(
                category = category.name,
                questionTitle = title,
                questionContent = content,
                imageKey = imageKey,
            ),
        )

    override suspend fun deleteInquiry(inquiryId: Long) =
        dataSource.deleteInquiry(inquiryId)

    override suspend fun restoreInquiry(inquiryId: Long) =
        dataSource.restoreInquiry(inquiryId)

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

    private companion object {
        /** 문의 첨부 사진은 [InquiryImageDataSource]가 늘 JPEG로 만든다. */
        const val IMAGE_CONTENT_TYPE = "image/jpeg"
    }
}
