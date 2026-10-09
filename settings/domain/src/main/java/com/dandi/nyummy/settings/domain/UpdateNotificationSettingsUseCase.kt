package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.settings.entity.NotificationSettingsVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 알림 설정 저장. 토글 하나 실패에 다이얼로그는 무거워서, 로그인 만료만 공통으로 알리고
 * 나머지 실패는 화면이 "다시 시도" 스낵바로 알린다.
 */
class UpdateNotificationSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(settings: NotificationSettingsVO): Result<NotificationSettingsVO> = try {
        Result.success(repository.updateNotificationSettings(settings))
    } catch (e: HttpResponseException) {
        if (e.isSessionExpired()) executeCommonErrorHanding(e)
        Result.failure(e)
    }
}
