package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 평소 식사 시각을 서버에 저장한다. 서버는 이 시각으로 식사 알림을 보낸다.
 * 실패하면 안내하고 [Result.failure]를 돌려준다. 다시 시도는 [onRetry]로 화면에 맡긴다.
 * 저장되면 기기 홈 화면 위젯에도 새 시각을 넘긴다.
 */
class SaveMealTimesUseCase @Inject constructor(
    private val repository: SettingsRepository,
    private val widgetHelper: WidgetHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealTimes: MealTimesVO, onRetry: () -> Unit): Result<Unit> {
        // 서버가 거절하기 전에 막는다. 같은 시각이면 식사 알림이 한 끼니만 나간다.
        if (mealTimes.hasDuplicateHours) {
            showError(resourceHelper.getString(StringResource.DUPLICATE_MEAL_TIMES))
            return Result.failure(IllegalArgumentException(resourceHelper.getString(StringResource.DUPLICATE_MEAL_TIMES)))
        }
        return request(
            SettingsErrorType.entries,
            onBusinessError = { type, cause ->
                showError(resourceHelper.getString(type.errorMsg))
                Result.failure(cause)
            },
            // 네트워크 끊김, 등록하지 않은 응답 오류, 예상 못 한 예외.
            onUnknownError = { showSaveFailed(onRetry) },
        ) {
            repository.saveMealTimes(mealTimes)
            widgetHelper.onMealTimesSaved(mealTimes)
        }
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    private fun showSaveFailed(onRetry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = resourceHelper.getString(StringResource.SETTINGS_MEAL_TIMES_SAVE_FAILED),
            callToActionText = resourceHelper.getString(StringResource.RETRY),
            onClickCTA = onRetry,
        )
    }
}
