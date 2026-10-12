package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 분석에 실패한 식사의 재분석 요청.
 * 화면의 식사가 서버와 달라 거절되면(이미 다시 분석했거나 지운 식사) [OutdatedMealException]으로 실패를 돌려준다.
 */
class ReanalyzeMealUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mealId: Long): Result<MealAnalysisStatus> = try {
        Result.success(repository.reanalyzeMeal(mealId))
    } catch (e: HttpResponseException) {
        var failure: Exception = e
        handleHttpError<HistoryErrorType>(
            e,
            onDomainError = { errorType ->
                showError(errorType.errorMsg)
                if (errorType in OUTDATED_MEAL_ERRORS) failure = OutdatedMealException()
            },
            onUnknownError = { showError(REANALYZE_ERROR_MESSAGE) },
        )
        Result.failure(failure)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 네트워크 끊김 등 HTTP 응답 전 실패도 앱이 죽지 않게 실패로 돌려준다.
        showError(REANALYZE_ERROR_MESSAGE)
        Result.failure(e)
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    private companion object {
        const val REANALYZE_ERROR_MESSAGE = "다시 분석하지 못했어요"

        /** 화면의 식사 상태가 서버와 달라서 난 오류. 목록을 새로 받으면 지금 상태가 보인다. */
        val OUTDATED_MEAL_ERRORS = setOf(HistoryErrorType.ANALYSIS_NOT_RETRYABLE, HistoryErrorType.MEAL_NOT_FOUND)
    }
}

/** 화면에 보이던 식사가 서버와 달라졌다(이미 다시 분석했거나 지운 식사). 화면은 목록을 새로 받아야 한다. */
class OutdatedMealException : Exception("Meal on screen is outdated")
