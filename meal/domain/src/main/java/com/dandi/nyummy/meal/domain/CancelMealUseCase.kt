package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 기록 직후의 식사를 취소한다. 사용자는 취소를 누른 뒤 바로 홈으로 가므로 실패해도 따로 안내하지 않는다
 * (취소하지 못한 식사는 서버가 5분 뒤 찾은 음식 전부로 계산하고, 기록 탭에서 지울 수 있다). 로그인 만료만 공통 처리한다.
 * 이미 지워진 식사(404)는 취소된 것과 같으므로 성공이다.
 */
class CancelMealUseCase @Inject constructor(
    private val repository: MealFoodRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): Result<Unit> = request(
        MealErrorType.entries,
        commonErrors = CommonErrorPolicy.SESSION_ONLY,
        onBusinessError = { type, cause -> if (type == MealErrorType.MEAL_NOT_FOUND) Result.success(Unit) else Result.failure(cause) },
    ) {
        repository.cancelMeal(mealId)
    }
}
