package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.mailbox.entity.RewardVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 안 받은 우편 모두 받기. 화면에 불러오지 않은 우편까지 서버가 한 번에 받고 보상의 합계를 돌려준다.
 * 실패는 공통 오류만 안내하고 돌려준다. 화면이 받지 못했다고 안내한다.
 */
class ClaimAllMailsUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(): Result<RewardVO> =
        request(MailErrorType.entries) { repository.claimAllMails() }
}
