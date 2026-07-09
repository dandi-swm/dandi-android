package com.swm.dandi.meal.presentation

import com.swm.dandi.common.presentation.mvi.UiState
import com.swm.dandi.meal.entity.MealAnalysisStatusVO

data class NewFoodUIState(
    val foodName: String = "",
    val isPhotoAttached: Boolean = false,
    val eatenHour: Int? = 12,
    val eatenMinute: Int? = 30,
    val foodNameError: String = "",
    val photoError: String = "",
    val eatenTimeError: String = "",
    val submitResultMessage: String = "",
    val isSubmitting: Boolean = false,
    val isAnalyzing: Boolean = false,
    val analysisMealRecordId: String = "",
    val analysisStatus: MealAnalysisStatusVO = MealAnalysisStatusVO.UNKNOWN,
    val analysisProgress: Float = 0f,
) : UiState {
    val hasEatenTime: Boolean
        get() = eatenHour != null && eatenMinute != null

    val eatenTimeLabel: String
        get() = if (eatenHour == null || eatenMinute == null) {
            ""
        } else {
            "${eatenHour.toTwoDigits()}:${eatenMinute.toTwoDigits()}"
        }

    val mealSessionLabel: String
        get() = when (eatenHour) {
            null -> "시간 선택 필요"
            in 5..10 -> "아침 식사"
            in 11..15 -> "점심 식사"
            in 16..20 -> "저녁 식사"
            else -> "간식"
        }

    val isReadyToSubmit: Boolean
        get() = !isSubmitting && foodName.trim().isNotEmpty() && isPhotoAttached && hasEatenTime

    val analysisTitle: String
        get() = when (analysisStatus) {
            MealAnalysisStatusVO.COMPLETED -> "영양 분석이\n완료됐어요."
            MealAnalysisStatusVO.FAILED -> "영양 분석에\n실패했어요."
            else -> "영양 성분을\n분석하고 있어요."
        }

    val analysisDescription: String
        get() = when (analysisStatus) {
            MealAnalysisStatusVO.COMPLETED -> "분석 결과가 준비됐습니다."
            MealAnalysisStatusVO.FAILED -> "기록은 저장되어 있습니다."
            else -> "잠시만 기다려주세요."
        }

    val analysisSubDescription: String
        get() = when (analysisStatus) {
            MealAnalysisStatusVO.COMPLETED -> "오늘 영양 현황에 반영됩니다."
            MealAnalysisStatusVO.FAILED -> "나중에 다시 확인해 주세요."
            else -> "꼼꼼하게 확인하고 있답니다!"
        }

    companion object {
        val empty: NewFoodUIState = NewFoodUIState()
    }
}

private fun Int.toTwoDigits(): String = toString().padStart(length = 2, padChar = '0')
