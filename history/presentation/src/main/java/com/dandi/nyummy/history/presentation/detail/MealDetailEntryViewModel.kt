package com.dandi.nyummy.history.presentation.detail

import androidx.lifecycle.viewModelScope
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.presentation.mvi.MviIntent
import com.dandi.nyummy.common.presentation.mvi.ReducerEvent
import com.dandi.nyummy.common.presentation.mvi.MviViewModel
import com.dandi.nyummy.common.presentation.mvi.UiState
import com.dandi.nyummy.history.domain.GetMealDetailV2UseCase
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.history.entity.MealAnalysisStatus
import com.dandi.nyummy.history.entity.MealAnalysisVersion
import com.dandi.nyummy.history.entity.MealDetailV2VO
import com.dandi.nyummy.meal.domain.MealFoodPickPage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 분석 버전을 모르는 채 들어온 식사 상세(푸시, 링크)가 열 곳. */
sealed interface MealDetailDestination {
    /** 기존 상세. */
    data object V1 : MealDetailDestination

    /** 음식 목록 상세. 조회한 [detail]을 그대로 넘겨 다시 조회하지 않게 한다. */
    data class V2(val detail: MealDetailV2VO) : MealDetailDestination

    /** 음식 확인을 기다리는 식사. 찾은 음식 고르기 화면으로 바꿔 연다. */
    data object FoodPick : MealDetailDestination

    /** 아직 보여 줄 상세가 없는 식사(인식 중, 실패). 기록 탭으로 돌아간다. */
    data object History : MealDetailDestination
}

/** 조회한 식사의 분석 버전과 상태로 열 곳을 정한다. */
fun mealDetailDestinationOf(detail: MealDetailV2VO): MealDetailDestination {
    val meal = detail.meal
    return when {
        meal.analysisVersion == MealAnalysisVersion.V1 -> MealDetailDestination.V1
        meal.status == MealAnalysisStatus.COMPLETED || meal.status.isCalculating -> MealDetailDestination.V2(detail)
        meal.status == MealAnalysisStatus.CONFIRMING -> MealDetailDestination.FoodPick
        else -> MealDetailDestination.History
    }
}

/**
 * @property destination 정해진 화면. null이면 아직 조회 중이거나 실패했습니다.
 * @property isLoadFailed 조회에 실패해 다시 시도를 보여 줍니다.
 */
data class MealDetailEntryUIState(
    val destination: MealDetailDestination? = null,
    val isLoadFailed: Boolean = false,
) : UiState

sealed interface MealDetailEntryIntent : MviIntent {
    data class Enter(val mealId: Long) : MealDetailEntryIntent
    data object RetryLoad : MealDetailEntryIntent
    data object ClickBack : MealDetailEntryIntent
}

sealed interface MealDetailEntryReducerEvent : ReducerEvent {
    data object LoadStarted : MealDetailEntryReducerEvent
    data object LoadFailed : MealDetailEntryReducerEvent
    data class Resolved(val destination: MealDetailDestination) : MealDetailEntryReducerEvent
}

/**
 * 분석 버전을 모르는 식사 상세 진입. v2 단건을 조회해 버전과 상태에 맞는 화면을 고른다.
 * 상세가 아닌 곳(고르기 화면, 기록 탭)으로 가야 하면 이 화면을 닫고 이동한다.
 */
@HiltViewModel
class MealDetailEntryViewModel @Inject constructor(
    private val getMealDetail: GetMealDetailV2UseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MealDetailEntryIntent, MealDetailEntryUIState, MealDetailEntryReducerEvent>(MealDetailEntryUIState()) {

    private var mealId: Long? = null

    override fun onIntent(intent: MealDetailEntryIntent) {
        when (intent) {
            is MealDetailEntryIntent.Enter -> {
                if (mealId == intent.mealId) return
                mealId = intent.mealId
                load()
            }
            MealDetailEntryIntent.RetryLoad -> load()
            MealDetailEntryIntent.ClickBack -> navigationHelper.navigateToBack()
        }
    }

    override fun reduce(state: MealDetailEntryUIState, event: MealDetailEntryReducerEvent): MealDetailEntryUIState =
        when (event) {
            MealDetailEntryReducerEvent.LoadStarted -> state.copy(isLoadFailed = false)
            MealDetailEntryReducerEvent.LoadFailed -> state.copy(isLoadFailed = true)
            is MealDetailEntryReducerEvent.Resolved -> state.copy(destination = event.destination, isLoadFailed = false)
        }

    private fun load() {
        val id = mealId ?: return
        dispatch(MealDetailEntryReducerEvent.LoadStarted)
        viewModelScope.launch {
            getMealDetail(id)
                .onSuccess { open(id, mealDetailDestinationOf(it)) }
                .onFailure { dispatch(MealDetailEntryReducerEvent.LoadFailed) }
        }
    }

    private fun open(mealId: Long, destination: MealDetailDestination) {
        when (destination) {
            MealDetailDestination.V1, is MealDetailDestination.V2 ->
                dispatch(MealDetailEntryReducerEvent.Resolved(destination))
            // 이 화면을 닫고 기록 탭 위에 고르기 화면을 연다. 뒤로 가면 기록 탭이다.
            MealDetailDestination.FoodPick -> {
                closeToHistory()
                navigationHelper.navigateTo(MealFoodPickPage(mealId))
            }
            MealDetailDestination.History -> closeToHistory()
        }
    }

    /**
     * 이 화면을 닫고 기록 탭을 연다. 앱이 켜진 채 링크로 들어오면 아래 화면이 기록 탭이 아닐 수 있어(홈 등)
     * 뒤로 가기만으로는 기록 탭에 닿지 않는다. 기록 탭에서 들어왔으면 뒤로 간 것과 같다.
     */
    private fun closeToHistory() {
        navigationHelper.navigateToBack()
        navigationHelper.navigateTo(HistoryPage)
    }
}
