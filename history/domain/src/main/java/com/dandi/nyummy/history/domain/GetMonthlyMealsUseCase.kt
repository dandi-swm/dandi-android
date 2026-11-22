package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 월간 식사 캘린더 조회.
 *
 * 실패를 화면 안에서 보여 주고 다시 불러오게 하므로 스낵바는 띄우지 않는다.
 * 세션 만료 같은 공통 오류만 공통 처리로 보낸다.
 */
class GetMonthlyMealsUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(year: Int, month: Int): Result<HistoryCalendarVO> =
        // 불러오기 실패는 화면 안에서 보여 주므로, 공통 오류만 안내한다.
        request(HistoryErrorType.entries) { repository.getMonthlyCalendar(year, month) }
}
