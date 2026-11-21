package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.settings.entity.InquiryCategory
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 문의 보내기. 제목과 내용의 앞뒤 공백은 지우고 보낸다. 빈 칸 검사는 화면이 먼저 한다.
 * 붙인 사진([imagePath])이 있으면 먼저 올리고 그 이미지 키를 함께 보낸다.
 * 공통 오류만 안내한다. 그 밖의 실패(사진 파일이 캐시에서 지워졌거나 스토리지 업로드가 끊긴 경우 등)는 화면이 보내기 실패로 안내한다.
 */
class SendInquiryUseCase @Inject constructor(
    private val repository: InquiryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(
        category: InquiryCategory,
        title: String,
        content: String,
        imagePath: String? = null,
    ): Result<Unit> = request {
        val imageKey = imagePath?.let { repository.uploadInquiryImage(it) }
        repository.sendInquiry(category, title.trim(), content.trim(), imageKey)
    }
}
