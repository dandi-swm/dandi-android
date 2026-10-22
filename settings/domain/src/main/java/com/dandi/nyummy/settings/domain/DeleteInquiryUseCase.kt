package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(inquiryId: Long): Result<Unit> = try {
        repository.deleteInquiry(inquiryId)
        Result.success(Unit)
    } catch (e: HttpResponseException) {
        var alreadyGone = false
        handleHttpError<InquiryErrorType>(
            e,
            onDomainError = { if (it == InquiryErrorType.INQUIRY_NOT_FOUND) alreadyGone = true },
        )
        if (alreadyGone) Result.success(Unit) else Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려주고, 화면이 행을 되살린 뒤 안내한다.
        Result.failure(e)
    }
}
