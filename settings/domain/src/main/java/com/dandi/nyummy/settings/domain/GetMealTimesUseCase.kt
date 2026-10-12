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
 * 서버에 저장된 평소 식사 시각을 읽는다.
 * 읽지 못하면 안내하고 [Result.failure]를 돌려준다. 다시 시도는 [onRetry]로 화면에 맡긴다.
 */
class GetMealTimesUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(onRetry: () -> Unit): Result<MealTimesVO> = try {
        Result.success(repository.getMealTimes())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpResponseException) {
        handleHttpError<SettingsErrorType>(
            e,
            onDomainError = { messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = it.errorMsg) },
            onUnknownError = { showLoadFailed(onRetry) },
        )
        Result.failure(e)
    } catch (e: Exception) {
        // 네트워크 끊김 등. 던지면 화면 코루틴이 앱을 죽인다.
        showLoadFailed(onRetry)
        Result.failure(e)
    }

    private fun showLoadFailed(onRetry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = LOAD_FAILED_MESSAGE,
            callToActionText = RETRY_LABEL,
            onClickCTA = onRetry,
        )
    }

    private companion object {
        const val LOAD_FAILED_MESSAGE = "식사 시간을 불러오지 못했어요"
        const val RETRY_LABEL = "다시 시도"
    }
}
