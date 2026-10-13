package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 지운 문의 되살리기(스낵바 되돌리기). 서버는 소프트 삭제라 되살릴 수 있다.
 * 복구 API(`POST /api/v1/inquiries/{inquiryId}/restore`)는 서버에 추가 요청 중이라, 그 전까지는 실패로 끝난다.
 * 없는 문의(`api.inquiry.notFound`)를 포함해 공통 오류가 아닌 실패는 안내 없이 돌려주고, 화면이 행을 다시 빼고 안내한다.
 */
class RestoreInquiryUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(inquiryId: Long): Result<Unit> = try {
        repository.restoreInquiry(inquiryId)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        // 없는 문의도 404라 공통 처리에 먼저 맡기면 "준비 중인 기능" 안내 뒤 우편함 밖으로 나간다. code를 먼저 본다.
        handleHttpError<MailboxErrorType>(e, onDomainError = {})
        Result.failure(e)
    }
}
