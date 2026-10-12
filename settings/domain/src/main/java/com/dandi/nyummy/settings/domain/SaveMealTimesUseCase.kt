package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 평소 식사 시각을 서버에 저장한다. 서버는 이 시각으로 식사 알림을 보낸다.
 * 실패하면 안내하고 [Result.failure]를 돌려준다. 다시 시도는 [onRetry]로 화면에 맡긴다.
 */
class SaveMealTimesUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mealTimes: MealTimesVO, onRetry: () -> Unit): Result<Unit> {
        // 서버가 거절하기 전에 막는다. 같은 시각이면 식사 알림이 한 끼니만 나간다.
        if (mealTimes.hasDuplicateHours) {
            showError(DUPLICATE_MEAL_TIMES_MESSAGE)
            return Result.failure(IllegalArgumentException(DUPLICATE_MEAL_TIMES_MESSAGE))
        }
        return try {
            Result.success(repository.saveMealTimes(mealTimes))
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpResponseException) {
            handleHttpError<SettingsErrorType>(
                e,
                onDomainError = { showError(it.errorMsg) },
                onUnknownError = { showSaveFailed(onRetry) },
            )
            Result.failure(e)
        } catch (e: Exception) {
            // 네트워크 끊김 등. 던지면 화면 코루틴이 앱을 죽인다.
            showSaveFailed(onRetry)
            Result.failure(e)
        }
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    private fun showSaveFailed(onRetry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = SAVE_FAILED_MESSAGE,
            callToActionText = RETRY_LABEL,
            onClickCTA = onRetry,
        )
    }

    private companion object {
        const val SAVE_FAILED_MESSAGE = "식사 시간을 저장하지 못했어요"
        const val RETRY_LABEL = "다시 시도"
    }
}
