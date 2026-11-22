package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.meal.entity.FoundFoodVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 고른 음식 목록을 보낸 결과. */
enum class SubmitMealFoodsResult {
    /** 서버가 받았다. 받은 목록으로 영양 계산을 시작한다. */
    ACCEPTED,

    /** 더는 고를 수 없는 식사다(이미 영양 계산을 시작했거나 지워졌다). 안내는 이미 했고, 고르기 화면을 닫으면 된다. */
    CLOSED,
}

/**
 * 사용자가 고른 음식 목록을 보내 영양 계산을 시작시킨다.
 *
 * 고르는 사이 5분이 지나 서버가 계산을 시작했거나 식사가 지워졌으면 그 사실을 안내하고 [SubmitMealFoodsResult.CLOSED]를 돌려준다.
 * 그 밖의 실패는 "다시 시도" 안내 후 [Result.failure]를 돌려주므로 화면은 그대로 머문다.
 */
class SubmitMealFoodsUseCase @Inject constructor(
    private val repository: MealFoodRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long, foods: List<FoundFoodVO>): Result<SubmitMealFoodsResult> = request(
        MealErrorType.entries,
        onBusinessError = { type, cause ->
            when (type) {
                MealErrorType.MEAL_FOODS_NOT_UPDATABLE, MealErrorType.MEAL_NOT_FOUND -> {
                    showNotice(IconType.WARNING, type.errorMsg)
                    Result.success(SubmitMealFoodsResult.CLOSED)
                }
                else -> {
                    showNotice(IconType.ERROR, type.errorMsg)
                    Result.failure(cause)
                }
            }
        },
        onUnknownError = { showNotice(IconType.ERROR, StringResource.MEAL_FOODS_SUBMIT_FAILED) },
    ) {
        repository.submitFoods(mealId, foods)
        SubmitMealFoodsResult.ACCEPTED
    }

    private fun showNotice(iconType: IconType, message: StringResource) {
        messageHelper.showSnackBar(iconType = iconType, messageText = resourceHelper.getString(message))
    }
}
