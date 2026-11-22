package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 문의 지우기(서버는 소프트 삭제). 이미 없는 문의(`api.inquiry.notFound`)는 지우려던 결과와 같으므로 성공으로 본다.
 * 그 밖의 실패는 안내 없이 돌려주고, 화면이 지운 행을 되살린 뒤 안내한다.
 */
class DeleteInquiryUseCase @Inject constructor(
    private val repository: InquiryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    // 네트워크 끊김 등 응답 전 실패도 실패로 돌려주고, 화면이 행을 되살린 뒤 안내한다.
    suspend operator fun invoke(inquiryId: Long): Result<Unit> = request(
        InquiryErrorType.entries,
        onBusinessError = { type, cause -> if (type == InquiryErrorType.INQUIRY_NOT_FOUND) Result.success(Unit) else Result.failure(cause) },
    ) {
        repository.deleteInquiry(inquiryId)
    }
}
