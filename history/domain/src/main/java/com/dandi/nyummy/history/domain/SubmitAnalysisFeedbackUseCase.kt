package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * AI 영양 분석 평가 보내기.
 *
 * 별점은 반 칸 수([ratingHalfSteps], 1~10 = 0.5~5.0점)로 받는다. 이유는 고른 아쉬운 점과 직접 쓴 글을 합쳐 하나로 보내고,
 * 둘 다 없으면 보내지 않는다. 다른 기기에서 이미 평가했으면(409) 평가는 끝난 것이므로 성공으로 본다.
 * 성공하면 고맙다는 스낵바를, 실패하면 오류 스낵바를 띄운다. 범위를 벗어난 별점은 보내지 않고 다시 고르라고 안내한다.
 */
class SubmitAnalysisFeedbackUseCase @Inject constructor(
    private val repository: HistoryRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(
        mealId: Long,
        ratingHalfSteps: Int,
        reason: AnalysisFeedbackReason?,
        comment: String,
    ): Result<Unit> {
        if (ratingHalfSteps !in RATING_HALF_STEPS) {
            // 화면이 범위를 지켜 여기 올 일은 없지만, 어겨도 앱을 죽이지 않고 서버가 거절할 때와 같이 안내한다.
            showError(HistoryErrorType.INVALID_RATING.errorMsg)
            return Result.failure(IllegalArgumentException("rating half steps out of range: $ratingHalfSteps"))
        }
        return try {
            repository.submitAnalysisFeedback(mealId, ratingHalfSteps, feedbackReasonOf(reason, comment))
            showThanks()
            Result.success(Unit)
        } catch (e: HttpResponseException) {
            var alreadySubmitted = false
            handleHttpError<HistoryErrorType>(
                e,
                onDomainError = { type ->
                    if (type == HistoryErrorType.ANALYSIS_FEEDBACK_ALREADY_EXISTS) {
                        alreadySubmitted = true
                    } else {
                        showError(type.errorMsg)
                    }
                },
                onUnknownError = { showError(SUBMIT_FAILED_MESSAGE) },
            )
            if (alreadySubmitted) {
                showThanks()
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 네트워크 끊김 등 HTTP 응답 전 실패.
            showError(SUBMIT_FAILED_MESSAGE)
            Result.failure(e)
        }
    }

    private fun showThanks() {
        messageHelper.showSnackBar(iconType = IconType.SUCCESS, messageText = THANKS_MESSAGE)
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    companion object {
        /** 0.5 ~ 5.0점을 반 칸 수로 센 범위. */
        val RATING_HALF_STEPS = 1..10

        /** 서버가 받는 이유 최대 길이. */
        const val REASON_MAX_LENGTH = 500

        private const val SUBMIT_FAILED_MESSAGE = "평가를 보내지 못했어요. 잠시 후 다시 시도해 주세요."
        private const val THANKS_MESSAGE = "고마워요! 냐미가 더 잘 알아볼게요"
    }
}

/**
 * 고른 아쉬운 점과 직접 쓴 글(줄을 바꿔 뒤에 붙임)을 평가 이유 하나로 합친다.
 * 서버 한도까지만 남기고, 둘 다 비었으면 null.
 */
internal fun feedbackReasonOf(reason: AnalysisFeedbackReason?, comment: String): String? =
    listOf(reason?.label.orEmpty(), comment.trim())
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .take(SubmitAnalysisFeedbackUseCase.REASON_MAX_LENGTH)
        .trim()
        .ifEmpty { null }
