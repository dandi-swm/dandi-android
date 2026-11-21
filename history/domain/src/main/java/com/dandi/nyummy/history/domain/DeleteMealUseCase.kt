package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.handlingErrorOnUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
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
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): Result<Unit> = request(
        HistoryErrorType.entries,
        onBusinessError = { type, cause ->
            if (type == HistoryErrorType.MEAL_NOT_FOUND) {
                // 이미 지워진 기록이면 지운 것과 같으므로 성공으로 돌려 목록에서 뺀다.
                Result.success(Unit)
            } else {
                showError(type.errorMsg)
                Result.failure(cause)
            }
        },
        // 네트워크 끊김 등 응답 전 실패와 등록하지 않은 응답 오류.
        onUnknownError = { showError(StringResource.HISTORY_DELETE_FAILED) },
    ) {
        repository.deleteMeal(mealId)
    }

    private fun showError(message: StringResource) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(message))
    }
}
