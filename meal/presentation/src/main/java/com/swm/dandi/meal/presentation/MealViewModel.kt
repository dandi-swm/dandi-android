package com.swm.dandi.meal.presentation

import androidx.lifecycle.viewModelScope
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.presentation.mvi.MviViewModel
import com.swm.dandi.meal.domain.MealUseCase
import com.swm.dandi.meal.domain.NewFoodPage
import com.swm.dandi.meal.domain.PreviousMealPage
import com.swm.dandi.meal.entity.MealAnalysisStatusVO
import com.swm.dandi.meal.entity.MealSessionStatusVO
import com.swm.dandi.meal.entity.MealSessionStatusTypeVO
import com.swm.dandi.meal.entity.MealStatusSheetVO
import com.swm.dandi.meal.entity.MealTypeVO
import com.swm.dandi.meal.entity.NutrientRatioVO
import com.swm.dandi.meal.entity.NutrientTypeVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealViewModel @Inject constructor(
    private val mealUseCase: MealUseCase,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<MealIntent, MealUIState, MealReducerEvent>(
    MealUIState.empty,
) {
    private val analysisPollingJobs: MutableMap<String, Job> = mutableMapOf()

    override fun onIntent(intent: MealIntent) {
        when (intent) {
            MealIntent.ClickMealStatus -> {
                dispatch(MealReducerEvent.MealStatusSheetOpened)
                loadMealStatusSheet()
            }

            MealIntent.ClickMealStatusRecord -> dispatch(MealReducerEvent.MealStatusRecordTypeRequested)
            is MealIntent.ClickRetryMealAnalysis -> retryMealAnalysis(intent.mealRecordId)
            MealIntent.DismissMealStatus -> {
                cancelAnalysisPolling()
                dispatch(MealReducerEvent.MealStatusSheetDismissed)
            }
            MealIntent.ClickPreviousMeal -> {
                cancelAnalysisPolling()
                dispatch(MealReducerEvent.MealStatusSheetDismissed)
                navigationHelper.navigateTo(PreviousMealPage)
            }

            MealIntent.ClickNewFood -> {
                cancelAnalysisPolling()
                dispatch(MealReducerEvent.MealStatusSheetDismissed)
                navigationHelper.navigateTo(NewFoodPage)
            }
        }
    }

    override fun reduce(state: MealUIState, event: MealReducerEvent): MealUIState = when (event) {
        MealReducerEvent.MealStatusSheetOpened -> state.copy(
            showMealStatusSheet = true,
            mealStatusSheet = state.mealStatusSheet.copy(step = MealStatusSheetStep.Status),
            mealStatusLoadErrorMessage = "",
        )

        MealReducerEvent.MealStatusRecordTypeRequested -> state.copy(
            showMealStatusSheet = true,
            mealStatusSheet = state.mealStatusSheet.copy(step = MealStatusSheetStep.RecordType),
        )

        MealReducerEvent.MealStatusSheetDismissed -> state.copy(
            showMealStatusSheet = false,
            mealStatusSheet = state.mealStatusSheet.copy(step = MealStatusSheetStep.Status),
        )

        MealReducerEvent.MealStatusLoadStarted -> state.copy(
            isMealStatusLoading = true,
            mealStatusLoadErrorMessage = "",
        )

        is MealReducerEvent.MealStatusLoaded -> state.copy(
            mealStatusSheet = event.mealStatusSheet.copy(step = state.mealStatusSheet.step),
            isMealStatusLoading = false,
            mealStatusLoadErrorMessage = "",
        )

        is MealReducerEvent.MealStatusLoadFailed -> state.copy(
            isMealStatusLoading = false,
            mealStatusLoadErrorMessage = event.message,
        )

        is MealReducerEvent.MealStatusAnalysisChanged -> state.copy(
            mealStatusSheet = state.mealStatusSheet.copy(
                mealSessions = state.mealStatusSheet.mealSessions.map { session ->
                    if (session.mealRecordId == event.mealRecordId) {
                        session.withStatus(event.status)
                    } else {
                        session
                    }
                }.toPersistentList(),
            )
        )
    }

    private fun loadMealStatusSheet() {
        dispatch(MealReducerEvent.MealStatusLoadStarted)
        viewModelScope.launch {
            mealUseCase.getMealStatusSheet()
                .onSuccess { statusSheet ->
                    dispatch(MealReducerEvent.MealStatusLoaded(statusSheet.toUiState()))
                    startAnalysisPolling(statusSheet)
                }
                .onFailure {
                    dispatch(MealReducerEvent.MealStatusLoadFailed("식사 상태를 불러오지 못했습니다."))
                }
        }
    }

    private fun MealStatusSheetVO.toUiState(): MealStatusSheetUiState {
        val defaultState = MealStatusSheetUiState()
        val mealSessionUiStates = mealSessions.map { it.toUiState() }.toPersistentList()
        val nutrientUiStates = nutritionSummary.nutrients.map { it.toUiState() }.toPersistentList()

        return defaultState.copy(
            step = defaultState.step,
            mealSessions = mealSessionUiStates.takeIf { it.isNotEmpty() } ?: defaultState.mealSessions,
            nutrients = nutrientUiStates.takeIf { it.isNotEmpty() } ?: defaultState.nutrients,
        )
    }

    private fun MealSessionStatusVO.toUiState(): MealSessionCardUiState =
        MealSessionCardUiState(
            mealRecordId = mealRecordId,
            mealType = mealType.name,
            statusType = status,
            title = mealType.toDisplayLabel(),
            status = status.toDisplayLabel(),
            displayName = displayName,
            description = status.toDescription(displayName),
            isError = status == MealSessionStatusTypeVO.FAILED,
            canRetryAnalysis = status == MealSessionStatusTypeVO.FAILED && mealRecordId.isNotBlank(),
        )

    private fun retryMealAnalysis(mealRecordId: String) {
        if (mealRecordId.isBlank()) return
        dispatch(MealReducerEvent.MealStatusAnalysisChanged(mealRecordId, MealSessionStatusTypeVO.IN_PROGRESS))
        startAnalysisPolling(mealRecordId)
    }

    private fun startAnalysisPolling(statusSheet: MealStatusSheetVO) {
        statusSheet.mealSessions
            .filter { it.status.shouldPollAnalysis() && it.mealRecordId.isNotBlank() }
            .forEach { startAnalysisPolling(it.mealRecordId) }
    }

    private fun startAnalysisPolling(mealRecordId: String) {
        if (analysisPollingJobs[mealRecordId]?.isActive == true) return
        analysisPollingJobs[mealRecordId] = viewModelScope.launch {
            try {
                while (true) {
                    val analysis = mealUseCase.getNutritionAnalysis(mealRecordId)
                        .getOrElse {
                            dispatch(
                                MealReducerEvent.MealStatusAnalysisChanged(
                                    mealRecordId = mealRecordId,
                                    status = MealSessionStatusTypeVO.FAILED,
                                )
                            )
                            return@launch
                        }
                    val sessionStatus = analysis.analysisStatus.toMealSessionStatusTypeVO()
                    dispatch(
                        MealReducerEvent.MealStatusAnalysisChanged(
                            mealRecordId = mealRecordId,
                            status = sessionStatus,
                        )
                    )
                    if (!analysis.analysisStatus.shouldPollAnalysis()) return@launch
                    delay(ANALYSIS_POLL_INTERVAL_MILLIS)
                }
            } finally {
                analysisPollingJobs.remove(mealRecordId)
            }
        }
    }

    private fun cancelAnalysisPolling() {
        analysisPollingJobs.values.forEach { it.cancel() }
        analysisPollingJobs.clear()
    }

    private fun NutrientRatioVO.toUiState(): NutrientProgressUiState =
        NutrientProgressUiState(
            nutrientType = nutrientType.name,
            label = nutrientType.toDisplayLabel(),
            percent = "${(ratio * 100).toInt()}%",
            progress = ratio.coerceIn(0f, 1f),
        )

    private fun NutrientTypeVO.toDisplayLabel(): String =
        when (this) {
            NutrientTypeVO.CALORIE -> "칼로리"
            NutrientTypeVO.CARBOHYDRATE -> "탄수화물"
            NutrientTypeVO.PROTEIN -> "단백질"
            NutrientTypeVO.FAT -> "지방"
            NutrientTypeVO.SODIUM -> "나트륨"
            NutrientTypeVO.UNKNOWN -> ""
        }

    private fun MealTypeVO.toDisplayLabel(): String =
        when (this) {
            MealTypeVO.BREAKFAST -> "아침"
            MealTypeVO.LUNCH -> "점심"
            MealTypeVO.DINNER -> "저녁"
            MealTypeVO.SNACK -> "간식"
            MealTypeVO.UNKNOWN -> ""
        }

    private fun MealSessionStatusTypeVO.toDisplayLabel(): String =
        when (this) {
            MealSessionStatusTypeVO.RECORDED -> "기록됨"
            MealSessionStatusTypeVO.NOT_RECORDED -> "기록 전"
            MealSessionStatusTypeVO.PENDING -> "분석 대기"
            MealSessionStatusTypeVO.IN_PROGRESS -> "분석 중"
            MealSessionStatusTypeVO.FAILED -> "분석 실패"
            MealSessionStatusTypeVO.UNKNOWN -> ""
        }

    private fun MealSessionStatusTypeVO.toDescription(displayName: String): String =
        when (this) {
            MealSessionStatusTypeVO.RECORDED -> displayName.ifBlank { "식사 기록 완료" }
            MealSessionStatusTypeVO.NOT_RECORDED -> "식사를 기록해주세요"
            MealSessionStatusTypeVO.PENDING -> displayName.takeIf { it.isNotBlank() }?.let { "$it 분석 대기 중..." }
                ?: "분석을 준비하고 있어요"
            MealSessionStatusTypeVO.IN_PROGRESS -> displayName.takeIf { it.isNotBlank() }?.let { "$it 분석 중..." }
                ?: "이미지 분석 중..."
            MealSessionStatusTypeVO.FAILED -> "다시 시도해주세요"
            MealSessionStatusTypeVO.UNKNOWN -> ""
        }

    private fun MealSessionCardUiState.withStatus(statusType: MealSessionStatusTypeVO): MealSessionCardUiState =
        copy(
            statusType = statusType,
            status = statusType.toDisplayLabel(),
            description = statusType.toDescription(displayName),
            isError = statusType == MealSessionStatusTypeVO.FAILED,
            canRetryAnalysis = statusType == MealSessionStatusTypeVO.FAILED && mealRecordId.isNotBlank(),
        )

    private fun MealSessionStatusTypeVO.shouldPollAnalysis(): Boolean =
        this == MealSessionStatusTypeVO.PENDING || this == MealSessionStatusTypeVO.IN_PROGRESS

    private fun MealAnalysisStatusVO.shouldPollAnalysis(): Boolean =
        this == MealAnalysisStatusVO.PENDING || this == MealAnalysisStatusVO.IN_PROGRESS

    private fun MealAnalysisStatusVO.toMealSessionStatusTypeVO(): MealSessionStatusTypeVO =
        when (this) {
            MealAnalysisStatusVO.PENDING -> MealSessionStatusTypeVO.PENDING
            MealAnalysisStatusVO.IN_PROGRESS -> MealSessionStatusTypeVO.IN_PROGRESS
            MealAnalysisStatusVO.COMPLETED -> MealSessionStatusTypeVO.RECORDED
            MealAnalysisStatusVO.FAILED -> MealSessionStatusTypeVO.FAILED
            MealAnalysisStatusVO.UNKNOWN -> MealSessionStatusTypeVO.FAILED
        }

    private companion object {
        const val ANALYSIS_POLL_INTERVAL_MILLIS = 1_500L
    }
}
