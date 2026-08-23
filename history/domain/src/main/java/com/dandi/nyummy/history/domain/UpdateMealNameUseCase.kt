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

/** 식사 이름 수정. */
class UpdateMealNameUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mealId: Long, name: String): Result<MealHistoryVO> = try {
        Result.success(repository.updateMealName(mealId, name))
    } catch (e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
        } else {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = EDIT_ERROR_MESSAGE)
        }
        Result.failure(e)
    }

    private companion object {
        const val EDIT_ERROR_MESSAGE = "이름 수정에 실패했어요"
    }
}
