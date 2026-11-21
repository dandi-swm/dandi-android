package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.settings.entity.InquiryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 문의 하나 조회. 없는 문의(`api.inquiry.notFound`)면 null로 성공한다(화면이 문의 내역으로 돌아간다).
 * 네트워크가 끊기면 안내하고 실패를 돌려준다.
 */
class GetInquiryDetailUseCase @Inject constructor(
    private val repository: InquiryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(inquiryId: Long): Result<InquiryVO?> = request(
        InquiryErrorType.entries,
        // 없는 문의는 공통 404 안내("준비 중인 기능") 대신 없는 것으로 돌려줘 화면이 문의 내역으로 돌아가게 한다.
        onBusinessError = { type, cause -> if (type == InquiryErrorType.INQUIRY_NOT_FOUND) Result.success(null) else Result.failure(cause) },
        // 전에는 응답 오류만 잡아 네트워크가 끊기면 앱이 종료됐다.
        onNetworkError = {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(StringResource.NETWORK_ERROR))
        },
    ) {
        repository.getInquiry(inquiryId)
    }
}
