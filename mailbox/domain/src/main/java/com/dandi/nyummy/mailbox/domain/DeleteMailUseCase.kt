package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 받은 우편 지우기. 서버는 삭제 표시만 해서 되돌릴 수 있다.
 * 이미 없는 우편이면 지운 것과 같으니 성공으로 본다. 그 밖의 실패는 화면이 행을 되돌리고 안내한다.
 */
class DeleteMailUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mailId: Long): Result<Unit> = request(
        MailErrorType.entries,
        onBusinessError = { type, cause -> if (type == MailErrorType.MAIL_NOT_FOUND) Result.success(Unit) else Result.failure(cause) },
    ) {
        repository.deleteMail(mailId)
    }
}
