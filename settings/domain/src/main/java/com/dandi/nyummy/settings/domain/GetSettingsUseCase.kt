package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.settings.entity.SettingsVO
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * 설정 화면 값(알림, 식사 시각)을 한 번에 읽는다.
 * 알림 설정을 읽지 못하면 안내하고 [Result.failure]를 돌려준다. 다시 시도는 [onRetry]로 화면에 맡긴다.
 */
class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(onRetry: () -> Unit): Result<SettingsVO> = try {
        Result.success(
            SettingsVO(
                notification = repository.getNotificationSettings(),
                mealTimes = getMealTimesOrNull(),
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) executeCommonErrorHanding(e) else showLoadFailed(onRetry)
        Result.failure(e)
    } catch (e: Exception) {
        // 네트워크 끊김 등. 던지면 화면 코루틴이 앱을 죽인다. 읽기 전에는 토글을 막으므로 다시 시도를 안내한다.
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

    /**
     * 식사 시각은 서버에서 읽는다. 설정 화면에서는 한 줄 요약일 뿐이라, 읽지 못해도 다른 설정은 보여 주도록 null로 둔다.
     * 로그인 만료만 안내한다. 화면에 돌아올 때마다 다시 읽으므로 다른 오류까지 다이얼로그로 띄우지 않는다.
     */
    private suspend fun getMealTimesOrNull(): MealTimesVO? = try {
        repository.getMealTimes()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpResponseException) {
        if (e.isSessionExpired()) executeCommonErrorHanding(e)
        null
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val LOAD_FAILED_MESSAGE = "설정을 불러오지 못했어요"
        const val RETRY_LABEL = "다시 시도"
    }
}
