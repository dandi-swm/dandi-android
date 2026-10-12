package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.history.entity.MealAnalysisStatus

/**
 * 서버 status 문자열(MealStatus)을 [MealAnalysisStatus] 로 변환한다.
 * 서버는 항상 status를 보낸다. 비어 있거나 모르는 값이면 [MealAnalysisStatus.UNKNOWN]으로 둔다.
 */
internal fun String?.toMealAnalysisStatus(): MealAnalysisStatus =
    when (this?.trim()?.uppercase()) {
        "WAITING" -> MealAnalysisStatus.WAITING
        "ANALYZING" -> MealAnalysisStatus.ANALYZING
        "COMPLETED" -> MealAnalysisStatus.COMPLETED
        "FAILED" -> MealAnalysisStatus.FAILED
        else -> MealAnalysisStatus.UNKNOWN
    }
