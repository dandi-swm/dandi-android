package com.swm.dandi.meal.presentation

import androidx.lifecycle.viewModelScope
import com.swm.dandi.common.domain.helper.NavigationHelper
import com.swm.dandi.common.presentation.mvi.MviViewModel
import com.swm.dandi.meal.domain.MealUseCase
import com.swm.dandi.meal.entity.CreateMealRequestVO
import com.swm.dandi.meal.entity.MealAnalysisStatusVO
import com.swm.dandi.meal.entity.MealInputModeVO
import com.swm.dandi.meal.entity.MealPhotoUploadUrlRequestVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class NewFoodViewModel @Inject constructor(
    private val mealUseCase: MealUseCase,
    private val navigationHelper: NavigationHelper,
) :
    MviViewModel<NewFoodIntent, NewFoodUIState, NewFoodReducerEvent>(
        NewFoodUIState.empty,
    ) {

    override fun onIntent(intent: NewFoodIntent) {
        when (intent) {
            NewFoodIntent.ClickBack -> navigationHelper.navigateToBack()

            is NewFoodIntent.ChangeFoodName -> {
                dispatch(NewFoodReducerEvent.FoodNameChanged(intent.foodName))
            }

            NewFoodIntent.TogglePhotoAttachment -> {
                dispatch(NewFoodReducerEvent.PhotoAttachmentChanged(!currentState.isPhotoAttached))
            }

            is NewFoodIntent.SelectEatenTime -> {
                dispatch(NewFoodReducerEvent.EatenTimeSelected(intent.hour, intent.minute))
            }

            NewFoodIntent.SelectCurrentTime -> selectCurrentTime()
            NewFoodIntent.Submit -> submit()
            NewFoodIntent.DismissAnalysis -> dispatch(NewFoodReducerEvent.AnalysisDismissed)
            NewFoodIntent.ClearSubmitResult -> dispatch(NewFoodReducerEvent.SubmitResultCleared)
        }
    }

    override fun reduce(
        state: NewFoodUIState,
        event: NewFoodReducerEvent,
    ): NewFoodUIState = when (event) {
        is NewFoodReducerEvent.FoodNameChanged -> state.copy(
            foodName = event.foodName,
            foodNameError = "",
            submitResultMessage = "",
            isSubmitting = false,
            isAnalyzing = false,
            analysisStatus = MealAnalysisStatusVO.UNKNOWN,
            analysisProgress = 0f,
        )

        is NewFoodReducerEvent.PhotoAttachmentChanged -> state.copy(
            isPhotoAttached = event.isPhotoAttached,
            photoError = "",
            submitResultMessage = "",
            isSubmitting = false,
            isAnalyzing = false,
            analysisStatus = MealAnalysisStatusVO.UNKNOWN,
            analysisProgress = 0f,
        )

        is NewFoodReducerEvent.EatenTimeSelected -> state.copy(
            eatenHour = event.hour,
            eatenMinute = event.minute,
            eatenTimeError = "",
            submitResultMessage = "",
            isSubmitting = false,
            isAnalyzing = false,
            analysisStatus = MealAnalysisStatusVO.UNKNOWN,
            analysisProgress = 0f,
        )

        is NewFoodReducerEvent.ValidationFailed -> state.copy(
            foodNameError = event.foodNameError,
            photoError = event.photoError,
            eatenTimeError = event.eatenTimeError,
            submitResultMessage = "",
            isSubmitting = false,
            isAnalyzing = false,
            analysisStatus = MealAnalysisStatusVO.UNKNOWN,
            analysisProgress = 0f,
        )

        NewFoodReducerEvent.SubmitStarted -> state.copy(
            foodNameError = "",
            photoError = "",
            eatenTimeError = "",
            submitResultMessage = "식사 기록을 저장하는 중입니다.",
            isSubmitting = true,
            isAnalyzing = false,
            analysisMealRecordId = "",
            analysisStatus = MealAnalysisStatusVO.UNKNOWN,
            analysisProgress = 0f,
        )

        is NewFoodReducerEvent.SubmitSucceeded -> state.copy(
            foodNameError = "",
            photoError = "",
            eatenTimeError = "",
            submitResultMessage = "${event.foodName} 기록을 저장했습니다.",
            isSubmitting = false,
            isAnalyzing = true,
            analysisMealRecordId = event.mealRecordId,
            analysisStatus = event.analysisStatus,
            analysisProgress = event.analysisStatus.toAnalysisProgress(),
        )

        is NewFoodReducerEvent.AnalysisLoaded -> state.copy(
            isAnalyzing = true,
            analysisStatus = event.analysisStatus,
            analysisProgress = event.analysisStatus.toAnalysisProgress(),
        )

        is NewFoodReducerEvent.AnalysisFailed -> state.copy(
            submitResultMessage = event.message,
            isSubmitting = false,
            isAnalyzing = true,
            analysisStatus = MealAnalysisStatusVO.FAILED,
            analysisProgress = MealAnalysisStatusVO.FAILED.toAnalysisProgress(),
        )

        is NewFoodReducerEvent.SubmitFailed -> state.copy(
            submitResultMessage = event.message,
            isSubmitting = false,
            isAnalyzing = false,
            analysisStatus = MealAnalysisStatusVO.UNKNOWN,
            analysisProgress = 0f,
        )

        NewFoodReducerEvent.AnalysisDismissed -> state.copy(
            isAnalyzing = false,
            isSubmitting = false,
            submitResultMessage = "영양 분석은 나중에 확인할 수 있습니다.",
        )

        NewFoodReducerEvent.SubmitResultCleared -> state.copy(submitResultMessage = "")
    }

    private fun selectCurrentTime() {
        val calendar = Calendar.getInstance()
        dispatch(
            NewFoodReducerEvent.EatenTimeSelected(
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE),
            )
        )
    }

    private fun submit() {
        val foodNameError = if (currentState.foodName.trim().isEmpty()) {
            "음식명을 입력해 주세요."
        } else {
            ""
        }
        val photoError = if (!currentState.isPhotoAttached) {
            "음식 사진을 첨부해 주세요."
        } else {
            ""
        }
        val eatenTimeError = if (!currentState.hasEatenTime) {
            "먹은 시간을 선택해 주세요."
        } else {
            ""
        }

        if (foodNameError.isNotEmpty() || photoError.isNotEmpty() || eatenTimeError.isNotEmpty()) {
            dispatch(
                NewFoodReducerEvent.ValidationFailed(
                    foodNameError = foodNameError,
                    photoError = photoError,
                    eatenTimeError = eatenTimeError,
                )
            )
            return
        }
        dispatch(NewFoodReducerEvent.SubmitStarted)
        val foodName = currentState.foodName.trim()
        val eatenAt = formatEatenAt(
            hour = currentState.eatenHour ?: return,
            minute = currentState.eatenMinute ?: return,
        )
        viewModelScope.launch {
            val uploadUrl = mealUseCase.getMealPhotoUploadUrl(MealPhotoUploadUrlRequestVO.skeleton)
                .getOrElse {
                    dispatch(NewFoodReducerEvent.SubmitFailed("사진 업로드 URL을 받아오지 못했습니다."))
                    return@launch
                }
            if (uploadUrl.photoId.isEmpty()) {
                dispatch(NewFoodReducerEvent.SubmitFailed("사진 업로드 식별자가 비어 있습니다."))
                return@launch
            }
            // TODO: 실제 이미지 파일이 연결되면 presigned URL로 S3 업로드를 수행한 뒤 식사 생성을 호출한다.
            val request = CreateMealRequestVO(
                inputMode = MealInputModeVO.NEW_FOOD,
                foodName = foodName,
                photoId = uploadUrl.photoId,
                eatenAt = eatenAt,
            )
            val meal = mealUseCase.createMeal(request)
                .getOrElse {
                    dispatch(NewFoodReducerEvent.SubmitFailed("식사 기록을 저장하지 못했습니다."))
                    return@launch
                }
            dispatch(
                NewFoodReducerEvent.SubmitSucceeded(
                    foodName = meal.displayName.ifEmpty { foodName },
                    mealRecordId = meal.mealRecordId,
                    analysisStatus = meal.analysisStatus,
                )
            )
            if (meal.mealRecordId.isEmpty()) {
                dispatch(NewFoodReducerEvent.AnalysisFailed("분석 조회에 필요한 식사 기록 id가 비어 있습니다."))
                return@launch
            }
            pollNutritionAnalysis(meal.mealRecordId)
        }
    }

    private suspend fun pollNutritionAnalysis(mealRecordId: String) {
        while (true) {
            val analysis = mealUseCase.getNutritionAnalysis(mealRecordId)
                .getOrElse {
                    dispatch(NewFoodReducerEvent.AnalysisFailed("영양 분석 상태를 확인하지 못했습니다."))
                    return
                }
            dispatch(NewFoodReducerEvent.AnalysisLoaded(analysis.analysisStatus))
            if (analysis.analysisStatus.isTerminal()) {
                return
            }
            delay(ANALYSIS_POLL_INTERVAL_MILLIS)
        }
    }

    private fun formatEatenAt(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return SimpleDateFormat(EATEN_AT_PATTERN, Locale.US).format(calendar.time)
    }

    private companion object {
        const val EATEN_AT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX"
        const val ANALYSIS_POLL_INTERVAL_MILLIS = 1_500L
    }
}

private fun MealAnalysisStatusVO.isTerminal(): Boolean =
    this == MealAnalysisStatusVO.COMPLETED || this == MealAnalysisStatusVO.FAILED

private fun MealAnalysisStatusVO.toAnalysisProgress(): Float =
    when (this) {
        MealAnalysisStatusVO.PENDING -> 0.16f
        MealAnalysisStatusVO.IN_PROGRESS -> 0.62f
        MealAnalysisStatusVO.COMPLETED -> 1f
        MealAnalysisStatusVO.FAILED -> 1f
        MealAnalysisStatusVO.UNKNOWN -> 0.08f
    }
