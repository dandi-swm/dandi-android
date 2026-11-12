package com.dandi.nyummy.home.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.home.entity.HomeSummaryVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TimelineCategory
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 홈 요약 조회. 홈에 들어올 때와 다른 화면에서 돌아올 때 부른다.
 * 공통 오류(401, 404, 5xx)는 공통 처리로 넘기고, 그 밖의 실패는 짧게 알린 뒤 [Result.failure]로 돌려준다.
 * 받으면 연속 기록일과 오늘 기록 수를 기기 홈 화면 위젯에 넘긴다.
 */
class GetHomeSummaryUseCase @Inject constructor(
    private val repository: HomeRepository,
    private val widgetHelper: WidgetHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<HomeSummaryVO> = try {
        ttiHelper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        val summary = try {
            repository.getHomeSummary()
        } finally {
            ttiHelper.endTTITimeline(TimelineCategory.API_RESPONSE_TIME)
        }
        widgetHelper.onHomeSummary(
            streakDays = summary.streakDays,
            todayRecordedCount = summary.todayRecordedCount,
        )
        Result.success(summary)
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(StringResource.HOME_LOAD_FAILED))
        }
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(StringResource.HOME_LOAD_FAILED))
        Result.failure(e)
    }
}
