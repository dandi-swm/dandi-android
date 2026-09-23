package com.dandi.nyummy.history.data.util

import com.dandi.nyummy.history.entity.MealAnalysisStatus

/**
 * 서버 status 문자열을 [MealAnalysisStatus] 로 변환한다.
 *
 * status 가 비어 있거나 알 수 없는 값이면 [name]/[calorieKcal] 로 보정한다 —
 * 이름이 비고 열량이 0 인 기록은 분석에 실패한 것으로 간주해, 화면에 빈 카드가 뜨지 않게 한다.
 */
internal fun String?.toMealAnalysisStatus(name: String?, calorieKcal: Int?): MealAnalysisStatus =
    when (this?.trim()?.uppercase()) {
        "WAITING" -> MealAnalysisStatus.WAITING
        "ANALYZING" -> MealAnalysisStatus.ANALYZING
        "COMPLETED" -> MealAnalysisStatus.COMPLETED
        "FAILED" -> MealAnalysisStatus.FAILED
        "UNKNOWN" -> MealAnalysisStatus.UNKNOWN
        else -> if (name.isNullOrBlank() && (calorieKcal ?: 0) == 0) {
            MealAnalysisStatus.FAILED
        } else {
            MealAnalysisStatus.COMPLETED
        }
    }
