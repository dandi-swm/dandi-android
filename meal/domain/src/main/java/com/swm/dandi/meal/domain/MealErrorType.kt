package com.swm.dandi.meal.domain

import com.swm.dandi.common.domain.error.HttpErrorType

enum class MealErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    INPUT_MODE_REQUIRED(
        type = "api.meal.inputModeRequired",
        errorMsg = "식사 기록 방식을 확인해 주세요.",
    ),
    PREVIOUS_FOOD_REQUIRED(
        type = "api.meal.previousFoodRequired",
        errorMsg = "이전 음식을 선택해 주세요.",
    ),
    FOOD_NAME_REQUIRED(
        type = "api.meal.foodNameRequired",
        errorMsg = "음식명을 입력해 주세요.",
    ),
    PHOTO_REQUIRED(
        type = "api.meal.photoRequired",
        errorMsg = "음식 사진을 첨부해 주세요.",
    ),
    EATEN_AT_REQUIRED(
        type = "api.meal.eatenAtRequired",
        errorMsg = "먹은 시간을 선택해 주세요.",
    ),
    PHOTO_UPLOAD_NOT_COMPLETED(
        type = "api.meal.photoUploadNotCompleted",
        errorMsg = "사진 업로드가 아직 완료되지 않았어요.",
    ),
    DUPLICATE_FOOD_NAME(
        type = "api.meal.duplicateFoodName",
        errorMsg = "이미 등록된 음식이에요. 이전 음식에서 선택해 주세요.",
    ),
}
