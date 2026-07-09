package com.swm.dandi.meal.data.dto

import com.swm.dandi.meal.entity.MealAnalysisStatusVO
import com.swm.dandi.meal.entity.MealInputModeVO
import com.swm.dandi.meal.entity.MealSessionStatusTypeVO
import com.swm.dandi.meal.entity.MealTypeVO
import com.swm.dandi.meal.entity.NutrientTypeVO

/**
 * 서버 문자열 코드값을 entity enum으로 정규화한다.
 *
 * 서버가 신규 코드값을 먼저 배포해도 앱이 크래시하지 않도록 알 수 없는 값은 `UNKNOWN`으로 둔다.
 */
internal fun String?.toMealInputModeVO(): MealInputModeVO =
    when (this) {
        MealInputModeVO.PREVIOUS_FOOD.name -> MealInputModeVO.PREVIOUS_FOOD
        else -> MealInputModeVO.NEW_FOOD
    }

internal fun String?.toMealTypeVO(): MealTypeVO =
    when (this) {
        MealTypeVO.BREAKFAST.name -> MealTypeVO.BREAKFAST
        MealTypeVO.LUNCH.name -> MealTypeVO.LUNCH
        MealTypeVO.DINNER.name -> MealTypeVO.DINNER
        MealTypeVO.SNACK.name -> MealTypeVO.SNACK
        else -> MealTypeVO.UNKNOWN
    }

internal fun String?.toMealSessionStatusTypeVO(): MealSessionStatusTypeVO =
    when (this) {
        MealSessionStatusTypeVO.RECORDED.name -> MealSessionStatusTypeVO.RECORDED
        MealSessionStatusTypeVO.NOT_RECORDED.name -> MealSessionStatusTypeVO.NOT_RECORDED
        MealSessionStatusTypeVO.PENDING.name -> MealSessionStatusTypeVO.PENDING
        MealSessionStatusTypeVO.IN_PROGRESS.name -> MealSessionStatusTypeVO.IN_PROGRESS
        MealSessionStatusTypeVO.FAILED.name -> MealSessionStatusTypeVO.FAILED
        else -> MealSessionStatusTypeVO.UNKNOWN
    }

internal fun String?.toNutrientTypeVO(): NutrientTypeVO =
    when (this) {
        NutrientTypeVO.CALORIE.name -> NutrientTypeVO.CALORIE
        NutrientTypeVO.CARBOHYDRATE.name -> NutrientTypeVO.CARBOHYDRATE
        NutrientTypeVO.PROTEIN.name -> NutrientTypeVO.PROTEIN
        NutrientTypeVO.FAT.name -> NutrientTypeVO.FAT
        NutrientTypeVO.SODIUM.name -> NutrientTypeVO.SODIUM
        else -> NutrientTypeVO.UNKNOWN
    }

internal fun String?.toMealAnalysisStatusVO(): MealAnalysisStatusVO =
    when (this) {
        MealAnalysisStatusVO.PENDING.name -> MealAnalysisStatusVO.PENDING
        MealAnalysisStatusVO.IN_PROGRESS.name -> MealAnalysisStatusVO.IN_PROGRESS
        MealAnalysisStatusVO.COMPLETED.name -> MealAnalysisStatusVO.COMPLETED
        MealAnalysisStatusVO.FAILED.name -> MealAnalysisStatusVO.FAILED
        else -> MealAnalysisStatusVO.UNKNOWN
    }
