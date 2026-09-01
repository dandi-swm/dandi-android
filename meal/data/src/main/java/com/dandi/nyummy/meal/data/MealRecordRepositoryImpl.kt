package com.dandi.nyummy.meal.data

import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.UploadImageUrlRequestDTO
import com.dandi.nyummy.meal.data.util.prepareMealPhotoFile
import com.dandi.nyummy.meal.domain.MealRecordRepository
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MealRecordRepositoryImpl(
    private val dataSource: MealRecordDataSource,
) : MealRecordRepository {

    override suspend fun prepareUploadImage(photoPath: String) = withContext(Dispatchers.IO) {
        prepareMealPhotoFile(photoPath)
    }

    override suspend fun issueImageUploadUrl(photoPath: String): MealImageUploadVO =
        dataSource.issueImageUploadUrl(
            UploadImageUrlRequestDTO(
                contentType = IMAGE_CONTENT_TYPE,
                fileSizeBytes = File(photoPath).length(),
            ),
        ).toVO()

    override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) {
        // Content-Type 은 RequestBody 가 전달하므로 헤더 목록에서 분리해 중복 전송을 막는다.
        val contentType = uploadTarget.uploadHeaders.entries
            .firstOrNull { it.key.equals(HEADER_CONTENT_TYPE, ignoreCase = true) }
            ?.value
            ?: IMAGE_CONTENT_TYPE
        val headers = uploadTarget.uploadHeaders
            .filterKeys { !it.equals(HEADER_CONTENT_TYPE, ignoreCase = true) }
        dataSource.uploadImage(
            uploadUrl = uploadTarget.uploadUrl,
            headers = headers,
            body = File(photoPath).asRequestBody(contentType.toMediaType()),
        )
    }

    override suspend fun createMeal(imageKey: String) =
        dataSource.createMeal(CreateMealRequestDTO(imageKey)).toVO()

    private companion object {
        const val IMAGE_CONTENT_TYPE = "image/jpeg"
        const val HEADER_CONTENT_TYPE = "Content-Type"
    }
}
