package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 아이템 탭 우편 목록 한 묶음 조회. 첫 묶음은 [cursor] 없이 읽고, 다음 묶음은 직전 묶음의 nextCursor로 읽는다.
 * 실패는 화면이 지금 목록을 그대로 두므로 공통 오류만 안내한다.
 */
class GetMailsUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(cursor: Long? = null, size: Int = PAGE_SIZE): Result<MailPageVO> =
        request(MailErrorType.entries) { repository.getMails(cursor, size.coerceIn(1, MAX_PAGE_SIZE)) }

    companion object {
        /** 한 번에 읽는 우편 수. */
        const val PAGE_SIZE = 10

        /** 서버가 한 번에 주는 최대 우편 수. */
        const val MAX_PAGE_SIZE = 50
    }
}
