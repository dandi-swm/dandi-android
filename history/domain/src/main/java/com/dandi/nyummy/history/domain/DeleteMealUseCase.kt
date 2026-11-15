package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
                onDomainError = { showError(resourceHelper.getString(it.errorMsg)) },
                onUnknownError = { showError(resourceHelper.getString(StringResource.HISTORY_DELETE_FAILED)) },
            )
            Result.failure(e)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        showError(resourceHelper.getString(StringResource.HISTORY_DELETE_FAILED))
        Result.failure(e)
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }
}
