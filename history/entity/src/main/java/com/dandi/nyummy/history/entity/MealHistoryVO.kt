package com.dandi.nyummy.history.entity

import kotlinx.serialization.Serializable

/**
 * 하루에 기록된 식사 1건입니다.
 *
 * @property id 식사 기록 식별자
 * @property name 음식 이름 (예: "치킨 샐러드"). 분석 실패/진행 중에는 비어 있습니다.
 * @property photoUrl 촬영 사진 URL. 백엔드 연동 전에는 빈 값입니다.
 * @property foodIconId 음식 픽셀 아이콘 식별자 (예: "salad")
 * @property recordedAt 촬영 시각 표시 문자열 (예: "08:10"). 서버 포맷 변환은 data 레이어 toVO() 에서 담당합니다.
 * @property calorieKcal 이 식사의 열량(kcal)
 * @property carbohydrateGram 이 식사의 탄수화물(g)
 * @property proteinGram 이 식사의 단백질(g)
 * @property fatGram 이 식사의 지방(g)
 * @property orderIndex 하루 안에서의 순서, 1부터 시작 ("첫 끼" 라벨용)
 * @property status AI 영양 분석 상태. 서버 값이 없으면 data 레이어에서 이름/열량으로 보정합니다.
 * @property catComment 이 식사에 대한 냐미의 한 줄 피드백. 단건 상세 조회에서만 내려오며 없으면 빈 값입니다.
 */
@Serializable
data class MealHistoryVO(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val foodIconId: String = "",
    val recordedAt: String = "",
    val calorieKcal: Int = 0,
    val carbohydrateGram: Int = 0,
    val proteinGram: Int = 0,
    val fatGram: Int = 0,
    val orderIndex: Int = 0,
    val status: MealAnalysisStatus = MealAnalysisStatus.COMPLETED,
    val catComment: String = "",
) {

    /** 분석이 끝나 이름/영양 정보를 믿을 수 있는 기록인지 여부. */
    val isAnalysisCompleted: Boolean
        get() = status == MealAnalysisStatus.COMPLETED

    /** 음식으로 인식하지 못해 재분석이 필요한 기록인지 여부. */
    val isAnalysisFailed: Boolean
        get() = status == MealAnalysisStatus.FAILED || status == MealAnalysisStatus.UNKNOWN

    /** 결과를 기다리는 중인 기록인지 여부. */
    val isAnalyzing: Boolean
        get() = status.isInProgress

    companion object {
        val empty = MealHistoryVO()
    }
}
