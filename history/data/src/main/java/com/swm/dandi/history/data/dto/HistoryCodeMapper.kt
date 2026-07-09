package com.swm.dandi.history.data.dto

import com.swm.dandi.history.entity.HistoryDailyNutritionEvaluationVO
import com.swm.dandi.history.entity.HistoryMealTypeVO
import com.swm.dandi.history.entity.HistoryNutrientTypeVO

/**
 * 서버 문자열 코드값을 history 일일 영양 평가 enum으로 정규화한다.
 *
 * 서버가 신규 코드값을 먼저 배포해도 앱이 크래시하지 않도록 알 수 없는 값은 `UNKNOWN`으로 둔다.
 */
internal fun String?.toHistoryDailyNutritionEvaluationVO(): HistoryDailyNutritionEvaluationVO =
    when (this) {
        HistoryDailyNutritionEvaluationVO.POSITIVE.name -> HistoryDailyNutritionEvaluationVO.POSITIVE
        HistoryDailyNutritionEvaluationVO.NEUTRAL.name -> HistoryDailyNutritionEvaluationVO.NEUTRAL
        HistoryDailyNutritionEvaluationVO.NEGATIVE.name -> HistoryDailyNutritionEvaluationVO.NEGATIVE
        HistoryDailyNutritionEvaluationVO.UNRECORDED.name -> HistoryDailyNutritionEvaluationVO.UNRECORDED
        else -> HistoryDailyNutritionEvaluationVO.UNKNOWN
    }

internal fun String?.toHistoryMealTypeVO(): HistoryMealTypeVO =
    when (this) {
        HistoryMealTypeVO.BREAKFAST.name -> HistoryMealTypeVO.BREAKFAST
        HistoryMealTypeVO.LUNCH.name -> HistoryMealTypeVO.LUNCH
        HistoryMealTypeVO.DINNER.name -> HistoryMealTypeVO.DINNER
        HistoryMealTypeVO.SNACK.name -> HistoryMealTypeVO.SNACK
        else -> HistoryMealTypeVO.UNKNOWN
    }

internal fun String?.toHistoryNutrientTypeVO(): HistoryNutrientTypeVO =
    when (this) {
        HistoryNutrientTypeVO.CALORIE.name -> HistoryNutrientTypeVO.CALORIE
        HistoryNutrientTypeVO.CARBOHYDRATE.name -> HistoryNutrientTypeVO.CARBOHYDRATE
        HistoryNutrientTypeVO.PROTEIN.name -> HistoryNutrientTypeVO.PROTEIN
        HistoryNutrientTypeVO.FAT.name -> HistoryNutrientTypeVO.FAT
        HistoryNutrientTypeVO.SODIUM.name -> HistoryNutrientTypeVO.SODIUM
        else -> HistoryNutrientTypeVO.UNKNOWN
    }
