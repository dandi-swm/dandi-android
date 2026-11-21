package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.meal.entity.MealFoodConfirmVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 기록 탭에서 확인 대기 식사를 눌렀을 때, 그 식사에서 찾은 음식 목록을 불러온다.
 *
 * 그 사이 서버가 영양 계산을 시작했으면 [MealFoodConfirmVO.isConfirmable]이 false로 온다(화면이 안내하고 닫는다).
 * 식사가 지워졌으면 그 사실을 안내하고 확인할 수 없는 식사로 돌려준다. 그 밖의 실패는 안내 후 [Result.failure]다.
 */
class GetFoundFoodsUseCase @Inject constructor(
    private val repository: MealFoodRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): Result<MealFoodConfirmVO> = request(
        MealErrorType.entries,
        onBusinessError = { type, cause ->
            if (type == MealErrorType.MEAL_NOT_FOUND) {
                showNotice(IconType.WARNING, StringResource.MEAL_FOODS_MEAL_GONE)
                Result.success(MealFoodConfirmVO.empty)
            } else {
                showNotice(IconType.ERROR, type.errorMsg)
                Result.failure(cause)
            }
        },
        onUnknownError = { showNotice(IconType.ERROR, StringResource.MEAL_FOODS_LOAD_FAILED) },
    ) {
        repository.getFoundFoods(mealId).also { confirm ->
            if (!confirm.isConfirmable) showNotice(IconType.WARNING, StringResource.MEAL_FOODS_ALREADY_ANALYZING)
        }
    }

    private fun showNotice(iconType: IconType, message: StringResource) {
        messageHelper.showSnackBar(iconType = iconType, messageText = resourceHelper.getString(message))
    }
}
