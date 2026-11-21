package com.dandi.nyummy.history.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.CommonErrorPolicy
import com.dandi.nyummy.common.domain.error.DomainFailure
import com.dandi.nyummy.common.domain.error.toDomainFailure
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodLineVO
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 음식 목록 방식(V2) 식사 UseCase들의 공통 처리. 실패하면 서버 code에 맞는 안내를, 없으면 [fallback] 안내를 스낵바로 띄우고
 * [Result.failure]를 돌려준다. 로그인 만료 같은 공통 오류는 공통 처리에 맡긴다.
 */
abstract class MealV2UseCase(
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    /**
     * @param treatAsSuccess 이 code로 실패하면 이미 원하는 상태라는 뜻이라 성공으로 본다(이미 지워진 것을 지우는 경우 등).
     */
    protected suspend fun <T> requestWithFallback(
        fallback: StringResource,
        treatAsSuccess: Pair<HistoryErrorType, T>? = null,
        block: suspend () -> T,
    ): Result<T> = request(
        HistoryErrorType.entries,
        onBusinessError = { type, cause ->
            if (treatAsSuccess != null && type == treatAsSuccess.first) {
                Result.success(treatAsSuccess.second)
            } else {
                showError(type.errorMsg)
                Result.failure(cause)
            }
        },
        // 네트워크 끊김 등 응답 전 실패와 등록하지 않은 응답 오류.
        onUnknownError = { showError(fallback) },
        block = block,
    )

    private fun showError(message: StringResource) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(message))
    }
}

/** 식사 단건 상세 조회 v2. 분석 버전과 음식 줄이 들어 있다. */
class GetMealDetailV2UseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : MealV2UseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): Result<MealDetailV2VO> =
        requestWithFallback(StringResource.HISTORY_MEAL_LOAD_FAILED) { repository.getMeal(mealId) }
}

/** 식사 대표 이름 수정 v2. 음식 줄과 영양은 바뀌지 않는다. */
class UpdateMealNameV2UseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : MealV2UseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long, name: String): Result<MealDetailV2VO> =
        requestWithFallback(StringResource.HISTORY_RENAME_FAILED) { repository.updateMealName(mealId, name) }
}

/** 식사 삭제 v2. 이미 지워진 기록이면 지운 것과 같으므로 성공이다. */
class DeleteMealV2UseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : MealV2UseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): Result<Unit> = requestWithFallback(
        fallback = StringResource.HISTORY_DELETE_FAILED,
        treatAsSuccess = HistoryErrorType.MEAL_NOT_FOUND to Unit,
    ) { repository.deleteMeal(mealId) }
}

/**
 * 음식 목록 전체를 보낸다. 순서를 바꾸거나(맨 위가 대표 음식) 음식을 추가할 때 쓴다.
 *
 * 돌려주는 상태가 [MealAnalysisStatus.COMPLETED]면 바로 다시 조회하면 되고, 그 밖이면(값을 새로 구해야 함)
 * [AwaitMealAnalysisUseCase]로 계산이 끝나기를 기다린다.
 */
class SaveMealFoodListUseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : MealV2UseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long, foods: List<MealFoodLineVO>): Result<MealAnalysisStatus> =
        requestWithFallback(StringResource.HISTORY_FOOD_EDIT_FAILED) { repository.submitFoods(mealId, foods) }
}

/**
 * 음식 한 줄을 고친다. 바꾼 항목이 없으면 서버에 보내지 않고 성공으로 돌려준다(서버는 빈 요청을 받지 않는다).
 * 응답에 본문이 없어 고친 뒤의 상태를 알 수 없으므로, 부르는 쪽이 식사를 다시 조회해 계산 중인지 본다.
 */
class UpdateMealFoodUseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : MealV2UseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long, mealFoodId: Long, change: MealFoodChangeVO): Result<Unit> =
        if (change.isEmpty) {
            Result.success(Unit)
        } else {
            requestWithFallback(StringResource.HISTORY_FOOD_EDIT_FAILED) { repository.updateFood(mealId, mealFoodId, change) }
        }
}

/** 음식 한 줄을 지운다. 이미 지워진 줄이면 지운 것과 같으므로 성공이다. */
class DeleteMealFoodUseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : MealV2UseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long, mealFoodId: Long): Result<Unit> = requestWithFallback(
        fallback = StringResource.HISTORY_FOOD_EDIT_FAILED,
        treatAsSuccess = HistoryErrorType.MEAL_FOOD_NOT_FOUND to Unit,
    ) { repository.deleteFood(mealId, mealFoodId) }
}

/** 영양 계산을 기다린 결과. */
enum class MealAnalysisWaitResult {
    /** 계산이 끝났다. 식사를 다시 조회하면 새 값이 있다. */
    COMPLETED,

    /** 계산에 실패했다. */
    FAILED,

    /** 기다리는 시간 안에 끝나지 않았다. 결과는 알림으로 온다. */
    TIMED_OUT,
}

/**
 * 영양 계산이 끝날 때까지 분석 상태를 [POLL_INTERVAL_MILLIS]마다 조회한다.
 *
 * 일시적인 실패(네트워크 끊김, 서버 오류)는 다음 주기에 다시 조회한다. [TIMEOUT_MILLIS]가 지나도 끝나지 않으면
 * [MealAnalysisWaitResult.TIMED_OUT]을 돌려준다. 로그인이 만료되면 공통 안내를 띄우고 기다리기를 그만둔다.
 * 화면이 사라져 코루틴이 취소되면 조회도 멈춘다. 안내는 화면이 한다.
 */
class AwaitMealAnalysisUseCase @Inject constructor(
    private val repository: MealV2Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(mealId: Long): MealAnalysisWaitResult {
        var waited = 0L
        while (waited < TIMEOUT_MILLIS) {
            delay(POLL_INTERVAL_MILLIS)
            waited += POLL_INTERVAL_MILLIS
            val status = try {
                repository.getAnalysisStatus(mealId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (val failure = e.toDomainFailure(HistoryErrorType.entries, CommonErrorPolicy.SESSION_ONLY)) {
                    // 로그인 만료만 공통 안내를 하고 기다리기를 그만둔다.
                    is DomainFailure.Common -> {
                        notifyFailure(failure)
                        return MealAnalysisWaitResult.TIMED_OUT
                    }
                    // 지워진 식사는 더 기다려도 끝나지 않는다.
                    is DomainFailure.Business -> if (failure.type == HistoryErrorType.MEAL_NOT_FOUND) return MealAnalysisWaitResult.FAILED
                    // 일시적인 실패(네트워크 끊김, 서버 오류)는 다음 주기에 다시 조회한다.
                    else -> Unit
                }
                continue
            }
            when (status) {
                MealAnalysisStatus.COMPLETED -> return MealAnalysisWaitResult.COMPLETED
                MealAnalysisStatus.FAILED -> return MealAnalysisWaitResult.FAILED
                else -> Unit
            }
        }
        return MealAnalysisWaitResult.TIMED_OUT
    }

    companion object {
        /** 분석 상태를 다시 묻는 간격. */
        const val POLL_INTERVAL_MILLIS = 2_000L

        /** 이만큼 기다려도 끝나지 않으면 그만 기다린다. */
        const val TIMEOUT_MILLIS = 60_000L
    }
}
