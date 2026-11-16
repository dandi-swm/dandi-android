package com.dandi.nyummy.history.presentation

import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.error.HttpResponseStatus
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.NavRoute
import com.dandi.nyummy.common.domain.navigation.NavSignal
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.history.domain.HistoryRepository
import com.dandi.nyummy.history.domain.MealV2Repository
import com.dandi.nyummy.history.entity.AnalysisFeedbackReason
import com.dandi.nyummy.history.entity.DailyMealHistoryVO
import com.dandi.nyummy.history.entity.DailyNutritionVO
import com.dandi.nyummy.history.entity.HistoryCalendarDayVO
import com.dandi.nyummy.history.entity.HistoryCalendarVO
import com.dandi.nyummy.history.entity.HistoryDateVO
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.history.entity.MealFoodChangeVO
import com.dandi.nyummy.history.entity.MealFoodLineVO
import com.dandi.nyummy.history.entity.MealHistoryVO
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TTIPage
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** 기록·식사 상세 ViewModel 테스트가 함께 쓰는 가짜 객체. */
internal class FakeHistoryRepository : HistoryRepository {
    val monthlyRequests = mutableListOf<Pair<Int, Int>>()
    val dailyRequests = mutableListOf<Triple<Int, Int, Int>>()
    val deleteRequests = mutableListOf<Long>()

    var monthlyOverride: suspend (Int, Int) -> HistoryCalendarVO =
        { year, month -> HistoryCalendarVO(year, month) }
    var dailyOverride: suspend (Int, Int, Int) -> DailyMealHistoryVO =
        { _, _, _ -> DailyMealHistoryVO.empty }
    var mealOverride: suspend (Long) -> MealHistoryVO =
        { mealId -> MealHistoryVO(id = mealId.toString()) }
    var updateOverride: suspend (Long, String) -> MealHistoryVO =
        { mealId, name -> MealHistoryVO(id = mealId.toString(), name = name) }
    var deleteOverride: suspend (Long) -> Unit = { }
    val reanalyzeRequests = mutableListOf<Long>()
    var reanalyzeOverride: suspend (Long) -> MealAnalysisStatus = { MealAnalysisStatus.ANALYZING }

    override suspend fun getMonthlyCalendar(year: Int, month: Int): HistoryCalendarVO {
        monthlyRequests += year to month
        return monthlyOverride(year, month)
    }

    override suspend fun getDailyMeals(year: Int, month: Int, day: Int): DailyMealHistoryVO {
        dailyRequests += Triple(year, month, day)
        return dailyOverride(year, month, day)
    }

    override suspend fun getMeal(mealId: Long): MealHistoryVO = mealOverride(mealId)

    override suspend fun updateMealName(mealId: Long, name: String): MealHistoryVO =
        updateOverride(mealId, name)

    override suspend fun reanalyzeMeal(mealId: Long): MealAnalysisStatus {
        reanalyzeRequests += mealId
        return reanalyzeOverride(mealId)
    }

    override suspend fun deleteMeal(mealId: Long) {
        deleteRequests += mealId
        deleteOverride(mealId)
    }

    val feedbackRequests = mutableListOf<Triple<Long, Int, String?>>()
    var feedbackOverride: suspend () -> Unit = { }

    override suspend fun submitAnalysisFeedback(mealId: Long, ratingHalfSteps: Int, reason: String?) {
        feedbackRequests += Triple(mealId, ratingHalfSteps, reason)
        feedbackOverride()
    }
}

internal class FakeNavigationHelper : NavigationHelper {
    val pages = mutableListOf<Page>()
    override val navigationFlow: Flow<NavSignal> = emptyFlow()
    override fun navigateByRoute(route: NavRoute) = Unit
    override fun navigateTo(page: Page) {
        pages += page
    }
    override fun navigateToAppLink(link: String, asRoot: Boolean) = Unit
    var backCount = 0
    override fun navigateToBack() {
        backCount++
    }
    override fun navigateToAsRoot(page: Page) = Unit
    override fun navigateToInitial() = Unit
    override fun navigateToExternalLink(url: String) = Unit
}

internal class FakeResourceHelper : ResourceHelper {
    override fun getString(resource: StringResource): String = ""
}

/** 기기에 기억한 고양이 이름이 없는 상태. */
internal class FakeCompanionMemoryHelper : CompanionMemoryHelper {
    override suspend fun lastSeenAtMillis(): Long? = null
    override suspend fun setLastSeenAtMillis(millis: Long) = Unit
    override suspend fun lastCatWeight(): String? = null
    override suspend fun setLastCatWeight(weight: String) = Unit
    override fun catName(): Flow<String?> = flowOf(null)
    override suspend fun setCatName(name: String) = Unit
    override suspend fun clear() = Unit
}

internal class FakeMessageHelper : MessageHelper {
    override val effect: Flow<MessageEffect> = emptyFlow()
    override fun showToast(toastMsg: String) = Unit
    override fun showToast(messageRes: Int) = Unit
    override fun showSnackBar(
        iconType: IconType,
        messageText: String,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) = Unit

    /** 리소스 번호로 띄운 스낵바. */
    val snackBarResources = mutableListOf<Int>()

    override fun showSnackBar(
        iconType: IconType,
        messageRes: Int,
        callToActionText: String?,
        onClickCTA: (() -> Unit)?,
    ) {
        snackBarResources += messageRes
    }

    override fun showOneButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        buttonText: String,
        onClickButton: (() -> Unit)?,
    ) = Unit

    override fun showTwoButtonDialog(
        titleText: String?,
        descText: String,
        cantIgnore: Boolean,
        leftButtonText: String,
        onClickLeftButton: (() -> Unit)?,
        rightButtonText: String,
        onClickRightButton: (() -> Unit)?,
    ) = Unit
}

internal class FakeTTIHelper : TTIHelper {
    override fun startTTITracking(page: TTIPage) = Unit
    override fun startTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITimeline(category: TimelineCategory) = Unit
    override fun endTTITracking() = Unit
    override fun shotTTILogging() = Unit
    override fun addTTIMetaData(metadata: TTIMetaData, value: Any?) = Unit
}

/**
 * 음식 목록 방식(V2) 식사 저장소. [detail]을 서버에 있는 식사로 보고, 수정 요청은 기록만 한다
 * (서버가 바꾼 결과는 테스트가 [detail]을 바꿔 흉내 낸다).
 */
internal class FakeMealV2Repository(var detail: MealDetailV2VO = MealDetailV2VO.empty) : MealV2Repository {
    var getRequests = 0
    var getOverride: (suspend () -> MealDetailV2VO)? = null
    val renameRequests = mutableListOf<String>()
    var renameOverride: suspend (String) -> Unit = { }
    val deleteMealRequests = mutableListOf<Long>()
    val submittedFoods = mutableListOf<List<MealFoodLineVO>>()
    var submitOverride: suspend (List<MealFoodLineVO>) -> MealAnalysisStatus = { MealAnalysisStatus.COMPLETED }
    val foodChanges = mutableListOf<Pair<Long, MealFoodChangeVO>>()
    var updateFoodOverride: suspend () -> Unit = { }
    val deletedFoods = mutableListOf<Long>()

    /** 분석 상태 조회가 차례로 돌려줄 값. 다 쓰면 COMPLETED다. */
    val statuses = ArrayDeque<MealAnalysisStatus>()
    var statusRequests = 0

    override suspend fun getMeal(mealId: Long): MealDetailV2VO {
        getRequests++
        return getOverride?.invoke() ?: detail
    }

    override suspend fun updateMealName(mealId: Long, name: String): MealDetailV2VO {
        renameOverride(name)
        renameRequests += name
        return detail
    }

    override suspend fun deleteMeal(mealId: Long) {
        deleteMealRequests += mealId
    }

    override suspend fun submitFoods(mealId: Long, foods: List<MealFoodLineVO>): MealAnalysisStatus {
        submittedFoods += foods
        return submitOverride(foods)
    }

    override suspend fun updateFood(mealId: Long, mealFoodId: Long, change: MealFoodChangeVO) {
        updateFoodOverride()
        foodChanges += mealFoodId to change
    }

    override suspend fun deleteFood(mealId: Long, mealFoodId: Long) {
        deletedFoods += mealFoodId
    }

    override suspend fun getAnalysisStatus(mealId: Long): MealAnalysisStatus {
        statusRequests++
        return statuses.removeFirstOrNull() ?: MealAnalysisStatus.COMPLETED
    }
}
