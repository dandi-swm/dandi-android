package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 일일 식사 목록 + 하루 영양 조회.
 *
 * 기록 탭과 홈 오늘 시트 모두 실패를 화면 안에서 보여 주고 다시 불러오게 하므로 스낵바는 띄우지 않는다.
 * 세션 만료 같은 공통 오류만 공통 처리로 보낸다.
 */
class GetDailyMealsUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(year: Int, month: Int, day: Int): Result<DailyMealHistoryVO> = try {
        Result.success(repository.getDailyMeals(year, month, day))
    } catch (e: HttpResponseException) {
        // 불러오기 실패는 화면 안에서 보여 주므로, 공통 오류만 안내한다.
        handleHttpError<HistoryErrorType>(e, onDomainError = {})
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        Result.failure(e)
    }
}
