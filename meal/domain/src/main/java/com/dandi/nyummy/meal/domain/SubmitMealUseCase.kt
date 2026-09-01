package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 촬영한 식사 사진을 제출한다.
 *
 * presigned URL 발급 → 스토리지 업로드 → 식사 생성(분석 시작)의 세 단계를 순서대로
 * 수행하며, 어느 단계에서 실패해도 동일한 실패 안내 후 [Result.failure] 를 돌려준다.
 */
class SubmitMealUseCase @Inject constructor(
    private val repository: MealRecordRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(photoPath: String): Result<CreatedMealVO> = try {
        val uploadTarget = repository.issueImageUploadUrl(photoPath)
        repository.uploadImage(uploadTarget, photoPath)
        Result.success(repository.createMeal(uploadTarget.imageKey))
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = SUBMIT_ERROR_MESSAGE)
        }
        Result.failure(e)
    }

    private companion object {
        const val SUBMIT_ERROR_MESSAGE = "식사를 기록하지 못했어요. 다시 시도해주세요"
    }
}
