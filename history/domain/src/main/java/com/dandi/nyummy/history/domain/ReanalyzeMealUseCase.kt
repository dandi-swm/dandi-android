package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isCommonErrorHandling
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 분석에 실패한 식사의 재분석 요청. */
class ReanalyzeMealUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mealId: Long): Result<MealHistoryVO> = try {
        Result.success(repository.reanalyzeMeal(mealId))
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showSnackBar(
                iconType = IconType.ERROR,
                messageText = REANALYZE_ERROR_MESSAGE,
            )
        }
        Result.failure(e)
    }

    private companion object {
        const val REANALYZE_ERROR_MESSAGE = "다시 분석하지 못했어요"
    }
}
