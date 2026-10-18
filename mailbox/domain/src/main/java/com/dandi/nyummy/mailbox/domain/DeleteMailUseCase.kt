package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mailId: Long): Result<Unit> = try {
        repository.deleteMail(mailId)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        var alreadyGone = false
        handleHttpError<MailErrorType>(
            e,
            onDomainError = { if (it == MailErrorType.MAIL_NOT_FOUND) alreadyGone = true },
        )
        if (alreadyGone) Result.success(Unit) else Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }
}
