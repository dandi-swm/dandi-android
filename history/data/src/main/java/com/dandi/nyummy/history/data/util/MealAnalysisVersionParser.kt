package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.history.entity.MealAnalysisVersion

/**
 * 서버 analysisVersion 문자열을 [MealAnalysisVersion]으로 변환한다.
 * 비어 있거나 모르는 값이면 기존 상세 화면으로 열리도록 [MealAnalysisVersion.V1]로 둔다.
 */
internal fun String?.toMealAnalysisVersion(): MealAnalysisVersion =
    when (this?.trim()?.uppercase()) {
        "V2" -> MealAnalysisVersion.V2
        else -> MealAnalysisVersion.V1
    }
