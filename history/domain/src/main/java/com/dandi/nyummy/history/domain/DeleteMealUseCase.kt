package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 식사 삭제. */
class DeleteMealUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mealId: Long): Result<Unit> = try {
        Result.success(repository.deleteMeal(mealId))
    } catch (e: HttpResponseException) {
        if (e.handlingErrorOnUseCase<HistoryErrorType>() == HistoryErrorType.MEAL_NOT_FOUND) {
            // 이미 지워진 기록이면 지운 것과 같으므로 성공으로 돌려 목록에서 뺀다.
            Result.success(Unit)
        } else {
            handleHttpError<HistoryErrorType>(
                e,
                onDomainError = { showError(it.errorMsg) },
                onUnknownError = { showError(DELETE_ERROR_MESSAGE) },
            )
            Result.failure(e)
        }
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    private companion object {
        const val DELETE_ERROR_MESSAGE = "삭제에 실패했어요"
    }
}
