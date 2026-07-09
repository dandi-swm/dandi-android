package com.swm.dandi.history.domain

import com.swm.dandi.common.domain.base.BaseUseCase
import com.swm.dandi.common.domain.error.HttpResponseException
import com.swm.dandi.common.domain.error.handlingErrorOnUseCase
import com.swm.dandi.common.domain.error.isCommonErrorHandling
import com.swm.dandi.common.domain.helper.MessageHelper
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.domain.helper.ResourceHelper
import com.swm.dandi.common.domain.message.IconType
import com.swm.dandi.history.entity.HistoryMealsVO
import com.swm.dandi.history.entity.HistoryVO
import com.swm.dandi.tti.TTIHelper
import javax.inject.Inject

class HistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend fun getHistory(year: Int, month: Int): Result<HistoryVO> =
        runCatching { historyRepository.getHistory(year, month) }
            .onFailure(::handleHistoryError)

    suspend fun getMeals(date: String): Result<HistoryMealsVO> =
        runCatching { historyRepository.getMeals(date) }
            .onFailure(::handleHistoryError)

    private fun handleHistoryError(throwable: Throwable) {
        val exception = throwable as? HttpResponseException ?: return
        if (exception.isCommonErrorHandling()) {
            executeCommonErrorHanding(exception)
            return
        }
        val historyError = exception.handlingErrorOnUseCase<HistoryErrorType>() ?: return
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = historyError.errorMsg,
        )
    }
}
