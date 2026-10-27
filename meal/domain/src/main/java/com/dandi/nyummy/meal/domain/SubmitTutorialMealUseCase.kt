package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.tti.TTIHelper
import java.io.IOException
import javax.inject.Inject

/** 튜토리얼 식사 기록 결과. */
sealed interface TutorialMealResult {
    /** 기록이 접수됐다. 평소처럼 [meal]로 연출을 이어 간다. */
    data class Accepted(val meal: CreatedMealVO) : TutorialMealResult

    /** 이미 튜토리얼 식사를 했다(다른 기기에서 먼저 마침). 연출 없이 돌아간다. */
    data object AlreadyCompleted : TutorialMealResult
}

/**
 * 첫 식사 튜토리얼에서 냐미가 준비한 예시 사진으로 식사를 기록한다. 사진은 올리지 않고, 서버가 보상 상자 우편을 보낸다.
 *
 * 접수되면 첫 식사 튜토리얼을 마치고 뒤 안내(우편함, 마이룸)를 시작한다. 이미 했다고 하면 첫 식사 튜토리얼만 마친다.
 * 그 밖의 실패는 평소 기록 실패처럼 안내하고 [Result.failure]를 돌려준다. 다음 홈 응답이 다시 튜토리얼을 띄운다.
 */
class SubmitTutorialMealUseCase @Inject constructor(
    private val repository: MealRecordRepository,
    private val tutorialHelper: TutorialHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<TutorialMealResult> = try {
        val meal = repository.createTutorialMeal()
        finishMealTutorial(startGuide = true)
        Result.success(TutorialMealResult.Accepted(meal))
    } catch (e: HttpResponseException) {
        var alreadyCompleted = false
        handleHttpError<MealErrorType>(
            e,
            onDomainError = {
                if (it == MealErrorType.TUTORIAL_ALREADY_COMPLETED) alreadyCompleted = true else showError(it.errorMsg)
            },
            onUnknownError = { showError(SUBMIT_ERROR_MESSAGE) },
        )
        if (alreadyCompleted) {
            finishMealTutorial(startGuide = false)
            Result.success(TutorialMealResult.AlreadyCompleted)
        } else {
            Result.failure(e)
        }
    } catch (e: java.util.concurrent.CancellationException) {
        throw e
    } catch (e: Exception) {
        showError(SUBMIT_ERROR_MESSAGE)
        Result.failure(e)
    }

    /**
     * 첫 식사 튜토리얼을 마쳤다고 남긴다. 서버는 이미 기록했으므로 기기에 남기지 못해도 실패로 보지 않는다
     * (다음 홈 응답이 다시 맞춘다).
     */
    private suspend fun finishMealTutorial(startGuide: Boolean) {
        try {
            tutorialHelper.setMealTutorialRequired(false)
            if (startGuide) tutorialHelper.start()
        } catch (_: IOException) {
        }
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    private companion object {
        const val SUBMIT_ERROR_MESSAGE = "식사를 기록하지 못했어요. 다시 시도해주세요"
    }
}
