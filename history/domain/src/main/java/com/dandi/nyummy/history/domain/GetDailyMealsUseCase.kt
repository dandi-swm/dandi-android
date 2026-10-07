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
import kotlin.coroutines.cancellation.CancellationException

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
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 같은 안내로 처리한다.
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = LOAD_ERROR_MESSAGE)
        Result.failure(e)
    }

    private companion object {
        const val LOAD_ERROR_MESSAGE = "식사 기록을 불러오지 못했어요"
    }
}
