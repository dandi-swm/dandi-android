package com.dandi.nyummy.mailbox.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 갤러리에서 고른 사진을 문의에 붙인다. 업로드할 JPEG로 바꿔 앱 캐시에 두고 그 경로를 돌려준다.
 * 실제 업로드는 보낼 때 한다(업로드 URL은 금방 만료된다). 안내 문구는 화면이 띄운다.
 */
class AttachInquiryImageUseCase @Inject constructor(
    private val repository: MailboxRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(photoUri: String): Result<String> = try {
        Result.success(repository.prepareInquiryImage(photoUri))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e as? InquiryImageLoadException ?: InquiryImageLoadException(e))
    }
}

/** 고른 사진을 열거나 문의 첨부용 JPEG로 바꾸지 못함. */
class InquiryImageLoadException(cause: Throwable? = null) : IllegalStateException(cause)
