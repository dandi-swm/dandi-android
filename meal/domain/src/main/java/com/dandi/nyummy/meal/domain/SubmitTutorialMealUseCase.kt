package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.isSessionExpired
import com.dandi.nyummy.common.domain.error.registeredErrorType
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.RewardBoxGrade
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
 * 첫 식사 튜토리얼의 식사를 기록한다. 냐미가 준비한 예시 사진을 화면에만 보여 주고 사진은 올리지 않는다.
 * 건너뛰기를 확정할 때도 이것으로 튜토리얼을 마쳤다고 서버에 남긴다(첫 보상 상자도 받는다).
 *
 * 응답 본문이 없어서 연출에 쓸 상자 등급은 서버 명세대로 골드로 둔다.
 * 접수되면 식사 튜토리얼을 마치고 뒤 단계(우편함, 마이룸, 냐미 건강 이야기)를 시작한다. 이미 했다고 하면 식사 튜토리얼만 마친다.
 *
 * 그 밖의 실패는 사유와 상관없이 "잠시 후 다시"로 안내하고 [Result.failure]를 돌려준다(로그인 만료만 공통 처리).
 * 튜토리얼 카드가 스낵바를 가리므로 안내는 코치마크보다 위 창에 뜨는 다이얼로그로 한다. 다음 홈 응답이 다시 튜토리얼을 띄운다.
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
        repository.createTutorialMeal()
        finishMealTutorial(startAfterMeal = true)
        Result.success(TutorialMealResult.Accepted(TutorialMeal))
    } catch (e: HttpResponseException) {
        when {
            e.registeredErrorType<MealErrorType>() == MealErrorType.TUTORIAL_ALREADY_COMPLETED -> {
                finishMealTutorial(startAfterMeal = false)
                Result.success(TutorialMealResult.AlreadyCompleted)
            }
            e.isSessionExpired() -> {
                executeCommonErrorHanding(e)
                Result.failure(e)
            }
            else -> {
                showRetryNotice()
                Result.failure(e)
            }
        }
    } catch (e: java.util.concurrent.CancellationException) {
        throw e
    } catch (e: Exception) {
        showRetryNotice()
        Result.failure(e)
    }

    /**
     * 식사 튜토리얼을 마쳤다고 남긴다. 서버는 이미 기록했으므로 기기에 남기지 못해도 실패로 보지 않는다
     * (다음 홈 응답이 다시 맞춘다).
     */
    private suspend fun finishMealTutorial(startAfterMeal: Boolean) {
        try {
            tutorialHelper.setMealTutorialRequired(false)
            if (startAfterMeal) tutorialHelper.startAfterMeal()
        } catch (_: IOException) {
        }
    }

    private fun showRetryNotice() {
        messageHelper.showOneButtonDialog(descText = RETRY_MESSAGE)
    }

    private companion object {
        /** 튜토리얼 식사 연출 값. 응답 본문이 없고 상자는 늘 골드다. */
        val TutorialMeal = CreatedMealVO(rewardBoxGrade = RewardBoxGrade.GOLD)
        const val RETRY_MESSAGE = "잠시 네트워크 환경이 불안정합니다. 잠시 후 다시 시도해주세요"
    }
}
