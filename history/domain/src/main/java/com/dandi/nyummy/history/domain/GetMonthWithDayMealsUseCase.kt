package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.HistoryMonthWithDayVO
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 월간 식사 캘린더와 고른 날의 식사 목록을 함께 조회.
 *
 * 두 요청을 동시에 보내되 실패 안내는 한 번만 한다. 한쪽이 실패하면 다른 쪽은 취소되고
 * 먼저 난 오류 하나만 공통 처리로 보낸다. 따로 부르면 같은 오류(로그인 만료 등)를 두 번 안내하게 된다.
 *
 * 실패를 화면 안에서 보여 주고 다시 불러오게 하므로 스낵바는 띄우지 않는다.
 * 세션 만료 같은 공통 오류만 공통 처리로 보낸다.
 */
class GetMonthWithDayMealsUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {
    suspend operator fun invoke(year: Int, month: Int, date: HistoryDateVO): Result<HistoryMonthWithDayVO> = try {
        coroutineScope {
            val calendar = async { repository.getMonthlyCalendar(year, month) }
            val dailyDetail = async { repository.getDailyMeals(date.year, date.month, date.day) }
            Result.success(HistoryMonthWithDayVO(calendar = calendar.await(), dailyDetail = dailyDetail.await()))
        }
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
