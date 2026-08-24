package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 일일 식사 목록 + 하루 영양 조회. */
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
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = LOAD_ERROR_MESSAGE)
        }
        Result.failure(e)
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE = "식사 기록을 불러오지 못했어요"
    }
}
