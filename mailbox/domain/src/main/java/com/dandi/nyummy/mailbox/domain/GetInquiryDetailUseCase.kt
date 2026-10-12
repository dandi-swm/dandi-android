package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.mailbox.entity.InquiryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 문의 하나 조회. 없는 문의(`api.inquiry.notFound`)면 null로 성공한다(화면이 우편함으로 돌아간다). */
class GetInquiryDetailUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(inquiryId: Long): Result<InquiryVO?> = try {
        Result.success(repository.getInquiry(inquiryId))
    } catch (e: HttpResponseException) {
        // 없는 문의는 공통 404 안내("준비 중인 기능") 대신 없는 것으로 돌려줘 화면이 우편함으로 돌아가게 한다.
        var notFound = false
        handleHttpError<MailboxErrorType>(
            e,
            onDomainError = { if (it == MailboxErrorType.INQUIRY_NOT_FOUND) notFound = true },
        )
        if (notFound) Result.success(null) else Result.failure(e)
    }
}
