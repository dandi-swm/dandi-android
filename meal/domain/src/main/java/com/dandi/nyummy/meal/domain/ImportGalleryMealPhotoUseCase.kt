package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
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
            ?: throw MealPhotoInvalidException(UNKNOWN_TAKEN_DATE_MESSAGE)
        if (takenAt.isoDate != KstTime.now(nowMillis).isoDate) {
            throw MealPhotoInvalidException(NOT_TAKEN_TODAY_MESSAGE)
        }
        Result.success(repository.importGalleryPhoto(photoUri))
    } catch (e: MealPhotoInvalidException) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = e.message)
        Result.failure(e)
    } catch (e: java.util.concurrent.CancellationException) {
        throw e
    } catch (e: Exception) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = IMPORT_ERROR_MESSAGE)
        Result.failure(e)
    }

    private companion object {
        const val NOT_TAKEN_TODAY_MESSAGE = "오늘 찍은 사진만 업로드 가능해요"
        const val UNKNOWN_TAKEN_DATE_MESSAGE = "촬영 날짜를 확인할 수 없어요. 오늘 찍은 사진만 업로드 가능해요"
        const val IMPORT_ERROR_MESSAGE = "사진을 불러오지 못했어요. 다시 시도해주세요"
    }
}
