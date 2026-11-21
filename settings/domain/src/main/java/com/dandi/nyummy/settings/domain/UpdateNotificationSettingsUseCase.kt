package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.settings.entity.NotificationSettingsChange
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
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    // 응답을 받기 전 실패(네트워크 끊김 등)도 화면이 되돌리고 "다시 시도"를 띄우도록 실패로 돌려준다.
    suspend operator fun invoke(change: NotificationSettingsChange): Result<Unit> =
        request(commonErrors = CommonErrorPolicy.SESSION_ONLY) { repository.updateNotificationSettings(change) }
}
