package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.mailbox.entity.RewardVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 우편 하나 받기(보상 상자 열기 포함).
 *
 * 성공하면 지급된 보상을 돌려준다. 이미 받았거나 없거나 기한이 지나 사라진 우편이면 `Result.success(null)`이고,
 * 화면은 목록을 새로 받아 지금 상태를 보여 준다. 그 밖의 실패는 공통 오류만 안내하고 실패를 돌려준다.
 */
class ClaimMailUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mailId: Long): Result<RewardVO?> = try {
        Result.success(repository.claimMail(mailId))
    } catch (e: HttpResponseException) {
        var alreadyGone = false
        handleHttpError<MailErrorType>(
            e,
            onDomainError = { if (it in ALREADY_GONE) alreadyGone = true },
        )
        if (alreadyGone) Result.success(null) else Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }

    private companion object {
        /** 받을 수 없게 된 우편. 다시 받을 수 없으니 실패가 아니라 "이미 처리됨"으로 본다. */
        val ALREADY_GONE = setOf(
            MailErrorType.MAIL_NOT_FOUND,
            MailErrorType.MAIL_ALREADY_CLAIMED,
            MailErrorType.MAIL_EXPIRED,
        )
    }
}
