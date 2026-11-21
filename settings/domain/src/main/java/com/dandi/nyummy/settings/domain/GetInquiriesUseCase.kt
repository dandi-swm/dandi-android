package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.settings.entity.InquiryPageVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 내가 보낸 문의 목록 한 묶음 조회. 첫 묶음은 [cursor] 없이 읽고, 다음 묶음은 직전 묶음의 nextCursor로 읽는다.
 * 실패하면 공통 오류와 네트워크 끊김만 안내하고 실패를 돌려준다. 화면은 받아 둔 목록을 그대로 둔다.
 */
class GetInquiriesUseCase @Inject constructor(
    private val repository: InquiryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(cursor: Long? = null, size: Int = PAGE_SIZE): Result<InquiryPageVO> = request(
        InquiryErrorType.entries,
        // 전에는 응답 오류만 잡아 네트워크가 끊기면 앱이 종료됐다.
        onNetworkError = { showNetworkError() },
    ) {
        repository.getInquiries(cursor, size.coerceIn(1, MAX_PAGE_SIZE))
    }

    private fun showNetworkError() {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(StringResource.NETWORK_ERROR))
    }

    companion object {
        /** 한 번에 읽는 문의 수. */
        const val PAGE_SIZE = 10

        /** 서버가 한 번에 주는 최대 문의 수. */
        const val MAX_PAGE_SIZE = 50
    }
}
