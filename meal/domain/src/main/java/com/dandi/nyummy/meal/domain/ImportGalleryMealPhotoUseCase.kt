package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.time.KstTime
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 갤러리에서 고른 사진을 식사 기록용 촬영본으로 가져온다.
 *
 * 사진 메타데이터의 촬영 시각이 오늘(KST)일 때만 앱 캐시로 복사해 그 파일 경로를 돌려준다.
 * 오늘 찍은 사진이 아니거나 촬영 시각을 확인할 수 없으면 안내 후 [Result.failure] 를 돌려준다.
 */
class ImportGalleryMealPhotoUseCase @Inject constructor(
    private val repository: MealRecordRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /** 테스트는 [nowMillis] 에 고정값을 넘긴다. */
    suspend operator fun invoke(
        photoUri: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): Result<String> = try {
        val takenAt = repository.readGalleryPhotoTakenAt(photoUri)
        when {
            takenAt == null -> reject(StringResource.MEAL_GALLERY_UNKNOWN_TAKEN_DATE)
            takenAt.isoDate != KstTime.now(nowMillis).isoDate ->
                reject(StringResource.MEAL_GALLERY_NOT_TAKEN_TODAY)
            else -> Result.success(repository.importGalleryPhoto(photoUri))
        }
    } catch (e: MealGalleryPhotoLoadException) {
        reject(StringResource.MEAL_GALLERY_LOAD_FAILED, e)
    } catch (e: java.util.concurrent.CancellationException) {
        throw e
    } catch (e: Exception) {
        reject(StringResource.MEAL_GALLERY_IMPORT_FAILED, e)
    }

    /** 현재 로케일의 안내 문구를 스낵바로 띄우고 실패로 돌려준다. */
    private fun reject(resource: StringResource, cause: Exception? = null): Result<String> {
        val message = resourceHelper.getString(resource)
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
        return Result.failure(cause ?: MealPhotoInvalidException(message))
    }
}

/** 갤러리에서 고른 사진을 열거나 앱 캐시로 복사·변환하지 못함. 안내 문구는 UseCase 가 정한다. */
class MealGalleryPhotoLoadException(cause: Throwable? = null) : IllegalStateException(cause)
