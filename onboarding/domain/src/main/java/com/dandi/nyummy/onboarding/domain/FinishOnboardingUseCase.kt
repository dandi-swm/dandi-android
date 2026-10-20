package com.dandi.nyummy.onboarding.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 온보딩 마무리. 지어 준 이름과 고른 평소 식사 시각으로 고양이를 만들고, 온보딩 완료를 기록한 뒤 홈을 루트로 이동한다.
 *
 * 서버는 이름과 식사 시각을 한 번에 받으므로 마지막 장면에서 한 번만 부른다. 완료 기록도 여기서 해야
 * 그 전에 앱이 꺼졌을 때 다음 실행에서 온보딩으로 돌아온다.
 * 이미 고양이가 있는 회원(409)은 만들어진 것으로 보고 마친다. 고양이를 만든 뒤 완료를 기록하기 전에 앱이 꺼져
 * 온보딩을 다시 하면 이렇게 된다.
 * 만들지 못하면 안내 후 [Result.failure]를 돌려주고, 화면은 식사 시각 장면에 머물러 다시 누를 수 있다.
 */
class FinishOnboardingUseCase @Inject constructor(
    private val repository: OnboardingRepository,
    private val analyticsHelper: AnalyticsHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(catName: String, mealTimes: MealTimesVO): Result<Unit> {
        // 서버가 거절하기 전에 막는다. 같은 시각이면 식사 알림이 한 끼니만 나간다.
        if (mealTimes.hasDuplicateHours) {
            messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = DUPLICATE_MEAL_TIMES_MESSAGE)
            return Result.failure(IllegalArgumentException(DUPLICATE_MEAL_TIMES_MESSAGE))
        }
        createCat(catName.trim(), mealTimes).onFailure { return Result.failure(it) }

        // 기기 저장이 실패해도 홈으로 간다. 고양이는 이미 만들었다. 식사 시각은 고양이와 함께 서버에 저장했다.
        runIgnoringFailure { repository.markOnboardingComplete() }
        analyticsHelper.logEvent(AnalyticsEvent.OnboardingComplete)
        navigationHelper.navigateToAsRoot(HomePage)
        return Result.success(Unit)
    }

    private suspend fun createCat(name: String, mealTimes: MealTimesVO): Result<Unit> = try {
        repository.createCat(name, mealTimes)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpResponseException) {
        var alreadyExists = false
        handleHttpError<OnboardingErrorType>(
            e,
            onDomainError = {
                when (it) {
                    OnboardingErrorType.CAT_ALREADY_EXISTS -> alreadyExists = true
                    OnboardingErrorType.INVALID_INPUT -> messageHelper.showOneButtonDialog(descText = it.errorMsg)
                }
            },
            onUnknownError = { messageHelper.showOneButtonDialog(descText = TEMPORARY_ERROR_MESSAGE) },
        )
        if (alreadyExists) Result.success(Unit) else Result.failure(e)
    } catch (e: Exception) {
        messageHelper.showOneButtonDialog(descText = TEMPORARY_ERROR_MESSAGE)
        Result.failure(e)
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

    private companion object {
        const val DUPLICATE_MEAL_TIMES_MESSAGE = "끼니마다 다른 시각을 골라주세요."
        const val TEMPORARY_ERROR_MESSAGE = "일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요."
    }
}
