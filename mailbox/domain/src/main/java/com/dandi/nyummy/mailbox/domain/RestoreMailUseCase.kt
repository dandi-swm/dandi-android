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
 * 지운 우편 되살리기(스낵바 되돌리기).
 * 공통 오류가 아닌 실패는 안내 없이 돌려주고, 화면이 행을 다시 빼고 안내한다.
 */
class RestoreMailUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mailId: Long): Result<Unit> = try {
        repository.restoreMail(mailId)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        // 없는 우편도 404라 공통 처리에 먼저 맡기면 "준비 중인 기능" 안내 뒤 우편함 밖으로 나간다. code를 먼저 본다.
        handleHttpError<MailErrorType>(e, onDomainError = {})
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }
}
