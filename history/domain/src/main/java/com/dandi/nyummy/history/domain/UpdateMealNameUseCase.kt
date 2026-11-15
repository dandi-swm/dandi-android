package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
        handleHttpError<HistoryErrorType>(
            e,
            onDomainError = { showError(resourceHelper.getString(it.errorMsg)) },
            onUnknownError = { showError(resourceHelper.getString(StringResource.HISTORY_RENAME_FAILED)) },
        )
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        showError(resourceHelper.getString(StringResource.HISTORY_RENAME_FAILED))
        Result.failure(e)
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }
}
