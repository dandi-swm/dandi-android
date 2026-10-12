package com.dandi.nyummy.meal.data

import com.dandi.nyummy.common.data.image.ImageUploadDataSource
import com.dandi.nyummy.common.data.image.ImageUploadPurpose
import com.dandi.nyummy.meal.data.dto.CreateMealRequestDTO
import com.dandi.nyummy.meal.data.dto.toVO
import com.dandi.nyummy.meal.data.util.prepareMealPhotoFile
import com.dandi.nyummy.meal.domain.MealRecordRepository
import com.dandi.nyummy.meal.entity.MealImageUploadVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MealRecordRepositoryImpl(
    private val dataSource: MealRecordDataSource,
    private val imageUploadDataSource: ImageUploadDataSource,
    private val galleryPhotoDataSource: MealGalleryPhotoDataSource,
) : MealRecordRepository {

    override suspend fun readGalleryPhotoTakenAt(photoUri: String) = withContext(Dispatchers.IO) {
        galleryPhotoDataSource.readTakenAt(photoUri)
    }

    override suspend fun importGalleryPhoto(photoUri: String) = withContext(Dispatchers.IO) {
        galleryPhotoDataSource.copyToCache(photoUri)
    }

    override suspend fun prepareUploadImage(photoPath: String, source: MealPhotoSource) = withContext(Dispatchers.IO) {
        prepareMealPhotoFile(photoPath, source)
    }

    override suspend fun issueImageUploadUrl(photoPath: String): MealImageUploadVO =
        imageUploadDataSource.issueUploadUrl(
            purpose = ImageUploadPurpose.MEAL,
            contentType = IMAGE_CONTENT_TYPE,
            fileSizeBytes = File(photoPath).length(),
        ).toVO()

    override suspend fun uploadImage(uploadTarget: MealImageUploadVO, photoPath: String) {
        imageUploadDataSource.upload(
            uploadUrl = uploadTarget.uploadUrl,
            uploadHeaders = uploadTarget.uploadHeaders,
            file = File(photoPath),
            defaultContentType = IMAGE_CONTENT_TYPE,
        )
    }

    override suspend fun createMeal(imageKey: String) =
        dataSource.createMeal(CreateMealRequestDTO(imageKey)).toVO()

    private companion object {
        const val IMAGE_CONTENT_TYPE = "image/jpeg"
    }
}
