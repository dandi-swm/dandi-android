package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.settings.entity.SettingsVO
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/** 설정 화면 값(배경음, 알림, 식사 시각)을 한 번에 읽는다. */
class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<SettingsVO> = try {
        Result.success(
            SettingsVO(
                isBgmEnabled = repository.isBgmEnabled(),
                notification = repository.getNotificationSettings(),
                mealTimes = repository.getMealTimes(),
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) executeCommonErrorHanding(e)
        Result.failure(e)
    } catch (e: Exception) {
        // 네트워크 끊김 등은 화면이 지금 값을 그대로 두도록 실패로 돌려준다. 던지면 화면 코루틴이 앱을 죽인다.
        Result.failure(e)
    }
}
