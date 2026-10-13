package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.mailbox.entity.InquiryPageVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 내가 보낸 문의 목록 한 묶음 조회. 첫 묶음은 [cursor] 없이 읽고, 다음 묶음은 직전 묶음의 nextCursor로 읽는다. */
class GetInquiriesUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(cursor: Long? = null, size: Int = PAGE_SIZE): Result<InquiryPageVO> = try {
        Result.success(repository.getInquiries(cursor, size.coerceIn(1, MAX_PAGE_SIZE)))
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) executeCommonErrorHanding(e)
        Result.failure(e)
    }

    companion object {
        /** 한 번에 읽는 문의 수. */
        const val PAGE_SIZE = 10

        /** 서버가 한 번에 주는 최대 문의 수. */
        const val MAX_PAGE_SIZE = 50
    }
}
