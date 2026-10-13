package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.mailbox.entity.InquiryCategory
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 문의 보내기. 제목과 내용의 앞뒤 공백은 지우고 보낸다. 빈 칸 검사는 화면이 먼저 한다. */
class SendInquiryUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(category: InquiryCategory, title: String, content: String): Result<Unit> = try {
        repository.sendInquiry(category, title.trim(), content.trim())
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) executeCommonErrorHanding(e)
        Result.failure(e)
    }
}
