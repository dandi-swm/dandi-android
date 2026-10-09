package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 지운 문의 되살리기(스낵바 되돌리기). 서버는 소프트 삭제라 되살릴 수 있다.
 * 공통 오류가 아닌 실패는 안내 없이 돌려주고, 화면이 행을 다시 빼고 안내한다.
 */
class RestoreInquiryUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(inquiryId: Long): Result<InquiryVO> = try {
        Result.success(repository.restoreInquiry(inquiryId))
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) executeCommonErrorHanding(e)
        Result.failure(e)
    }
}
