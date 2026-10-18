package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.mailbox.entity.MailPageVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(cursor: Long? = null, size: Int = PAGE_SIZE): Result<MailPageVO> = try {
        Result.success(repository.getMails(cursor, size.coerceIn(1, MAX_PAGE_SIZE)))
    } catch (e: HttpResponseException) {
        handleHttpError<MailErrorType>(e, onDomainError = {})
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }

    companion object {
        /** 한 번에 읽는 우편 수. */
        const val PAGE_SIZE = 10

        /** 서버가 한 번에 주는 최대 우편 수. */
        const val MAX_PAGE_SIZE = 50
    }
}
