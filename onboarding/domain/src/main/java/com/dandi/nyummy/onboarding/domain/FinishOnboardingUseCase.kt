package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 온보딩 마무리. 고른 평소 식사 시각을 저장하고 온보딩 완료를 기록한 뒤 홈을 루트로 이동한다.
 * 완료 기록을 여기서 해야 식사 시각 장면에서 앱이 꺼졌을 때 다음 실행에서 온보딩으로 돌아온다.
 */
class FinishOnboardingUseCase @Inject constructor(
    private val repository: OnboardingRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(mealTimes: MealTimesVO) {
        // 기기 저장이 실패해도 홈으로 간다. 여기서 막으면 홈에 갈 수 없고, 식사 시각은 나중에 다시 정할 수 있다.
        runIgnoringFailure { repository.saveMealTimes(mealTimes) }
        runIgnoringFailure { repository.markOnboardingComplete() }
        navigationHelper.navigateToAsRoot(HomePage)
    }

    private suspend fun runIgnoringFailure(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Unit
        }
    }
}
