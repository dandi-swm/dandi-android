package com.swm.dandi.meal.domain

import com.swm.dandi.common.domain.base.BaseUseCase
import com.swm.dandi.common.domain.error.HttpResponseException
import com.swm.dandi.common.domain.error.handlingErrorOnUseCase
import com.swm.dandi.common.domain.error.isCommonErrorHandling
import com.swm.dandi.common.domain.helper.MessageHelper
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.domain.message.IconType
import com.swm.dandi.meal.entity.CreateMealRequestVO
import com.swm.dandi.meal.entity.CreateMealVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlRequestVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlVO
import com.swm.dandi.meal.entity.MealStatusSheetVO
import com.swm.dandi.meal.entity.NutritionAnalysisVO
import com.swm.dandi.meal.entity.PreviousMealPageVO
import com.swm.dandi.tti.TTIHelper
import javax.inject.Inject

class MealUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend fun getMealStatusSheet(): Result<MealStatusSheetVO> =
        runCatching { mealRepository.getMealStatusSheet() }
            .onFailure(::handleMealError)

    suspend fun getPreviousMealPage(): Result<PreviousMealPageVO> =
        runCatching { mealRepository.getPreviousMealPage() }
            .onFailure(::handleMealError)

    suspend fun getMealPhotoUploadUrl(request: MealPhotoUploadUrlRequestVO): Result<MealPhotoUploadUrlVO> =
        runCatching { mealRepository.getMealPhotoUploadUrl(request) }
            .onFailure(::handleMealError)

    suspend fun createMeal(request: CreateMealRequestVO): Result<CreateMealVO> =
        runCatching { mealRepository.createMeal(request) }
            .onFailure(::handleMealError)

    suspend fun getNutritionAnalysis(mealRecordId: String): Result<NutritionAnalysisVO> =
        runCatching { mealRepository.getNutritionAnalysis(mealRecordId) }
            .onFailure(::handleMealError)

    private fun handleMealError(throwable: Throwable) {
        val exception = throwable as? HttpResponseException ?: return
        if (exception.isCommonErrorHandling()) {
            executeCommonErrorHanding(exception)
            return
        }
        val mealError = exception.handlingErrorOnUseCase<MealErrorType>() ?: return
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = mealError.errorMsg,
        )
    }
}
