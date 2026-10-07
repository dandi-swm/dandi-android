package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 홈 요약 조회. 홈에 들어올 때와 다른 화면에서 돌아올 때 부른다.
 * 공통 오류(401, 404, 5xx)는 공통 처리로 넘기고, 그 밖의 실패는 짧게 알린 뒤 [Result.failure]로 돌려준다.
 */
class GetHomeSummaryUseCase @Inject constructor(
    private val repository: HomeRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<HomeSummaryVO> = try {
        Result.success(repository.getHomeSummary())
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = LOAD_ERROR_MESSAGE)
        }
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = LOAD_ERROR_MESSAGE)
        Result.failure(e)
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE = "홈 정보를 불러오지 못했어요"
    }
}
