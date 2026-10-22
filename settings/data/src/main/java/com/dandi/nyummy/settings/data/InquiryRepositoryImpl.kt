package com.dandi.nyummy.settings.data

import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import com.dandi.nyummy.common.data.image.ImageUploadPurpose
import com.dandi.nyummy.settings.data.dto.CreateInquiryRequestDTO
import com.dandi.nyummy.settings.data.image.InquiryImageDataSource
import com.dandi.nyummy.settings.domain.InquiryRepository
import com.dandi.nyummy.settings.entity.InquiryCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class InquiryRepositoryImpl(
    private val remoteDataSource: InquiryRemoteDataSource,
    private val imageUploadDataSource: ImageUploadDataSource,
    private val inquiryImageDataSource: InquiryImageDataSource,
) : InquiryRepository {

    override suspend fun getInquiries(cursor: Long?, size: Int) =
        remoteDataSource.getInquiries(cursor, size).toVO()

    override suspend fun getInquiry(inquiryId: Long) =
        remoteDataSource.getInquiry(inquiryId).toVO()

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
        remoteDataSource.createInquiry(
            CreateInquiryRequestDTO(
                category = category.name,
                questionTitle = title,
                questionContent = content,
                imageKey = imageKey,
            ),
        )

    override suspend fun deleteInquiry(inquiryId: Long) =
        remoteDataSource.deleteInquiry(inquiryId)

    override suspend fun restoreInquiry(inquiryId: Long) =
        remoteDataSource.restoreInquiry(inquiryId)

    private companion object {
        /** 문의 첨부 사진은 [InquiryImageDataSource]가 늘 JPEG로 만든다. */
        const val IMAGE_CONTENT_TYPE = "image/jpeg"
    }
}
