package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/** 식사 이름 수정. */
class UpdateMealNameUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long, name: String): Result<MealHistoryVO> = request(
        HistoryErrorType.entries,
        onBusinessError = { type, cause ->
            showError(type.errorMsg)
            Result.failure(cause)
        },
        // 네트워크 끊김 등 응답 전 실패와 등록하지 않은 응답 오류.
        onUnknownError = { showError(StringResource.HISTORY_RENAME_FAILED) },
    ) {
        repository.updateMealName(mealId, name)
    }

    private fun showError(message: StringResource) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(message))
    }
}
